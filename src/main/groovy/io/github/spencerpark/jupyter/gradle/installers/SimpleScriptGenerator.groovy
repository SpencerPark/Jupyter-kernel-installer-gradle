package io.github.spencerpark.jupyter.gradle.installers

import java.util.regex.Matcher
import java.util.regex.Pattern

class SimpleScriptGenerator {
    static final Pattern VALID_TOKEN_NAME = Pattern.compile('^[a-zA-Z0-9_-]+$')

    private static final int FLAG_SMART = 1 << 1
    private static final int FLAG_RECURSIVE = 1 << 2

    private static String indentCode(String code, String indent) {
        return code.replace('\n', "\n$indent")
    }

    private static boolean isIndentChar(char c) {
        switch (c) {
            case ' ' as char:
            case '\t' as char:
                return true
            default:
                return false
        }
    }

    private final Map<String, ?> _tokens = new HashMap<>()

    // A cached pattern that is null if not yet baked or the tokens change
    private Pattern _tokenPattern = null

    private final StringBuilder _compiledTemplate = new StringBuilder()
    private final String _indentString
    private final String _newLine
    private int _indent = 0

    SimpleScriptGenerator(String indentString = '    ', String newLine = System.lineSeparator()) {
        this._indentString = indentString
        this._newLine = newLine
        for (int i = 0; i < indentString.length(); i++) {
            if (!isIndentChar(indentString.charAt(i)))
                throw new IllegalArgumentException("Indent string must only contain spaces or tabs but found ${indentString.charAt(i)}")
        }
    }

    SimpleScriptGenerator(SimpleScriptGenerator config) {
        this._indentString = config._indentString
        this._newLine = config._newLine
        this._tokens.putAll(config._tokens)
    }

    Pattern getTokenPattern() {
        if (this._tokenPattern == null) {
            String allTokens = this._tokens.keySet().join('|')
            this._tokenPattern = Pattern.compile('@(/(?<flags>[sr]+)/)?(?<token>' + allTokens + ')@')
        }
        return this._tokenPattern
    }

    private String getTokenValue(String token) {
        def value = this._tokens.get(token)
        if (value == null || !(value.metaClass.respondsTo(value, 'call')))
            return value.toString()

        SimpleScriptGenerator generator = new SimpleScriptGenerator(this)
        value(generator)

        return generator.compiled
    }

    void putToken(String name, Object value) {
        if (!VALID_TOKEN_NAME.matcher(name).matches())
            throw new IllegalArgumentException("Cannot use '$name' as a token name")

        String replaced = this._tokens.put(name, value)
        if (replaced == null) // This is a new token
            this._tokenPattern = null
    }

    void putTokens(Map<String, ?> tokens) {
        tokens.each(this.&putToken)
    }

    void indent() {
        this._indent++
    }

    void dedent() {
        if (this._indent > 0)
            this._indent--
    }

    void indented(Closure closure) {
        this.indent()
        closure()
        this.dedent()
    }

    void resetIndent() {
        this._indent = 0
    }

    void putIndent() {
        for (int i = 0; i < this._indent; i++)
            this._compiledTemplate.append(this._indentString)
    }

    private void putNecessaryIndent() {
        int lastNL = this._compiledTemplate.lastIndexOf('\n') + 1

        // Return early if there is a non-indent char before the end of the string signalling
        // that this line is partially written
        for (int i = lastNL; i < this._compiledTemplate.length(); i++) {
            if (!isIndentChar(this._compiledTemplate.charAt(i)))
                return
        }

        String indentString = this._indentString * this._indent

        // Replace the necessary indent
        for (int i = 0; i < indentString.length(); i++) {
            // If the current template index is past the end
            if ((lastNL + i) >= this._compiledTemplate.length()) {
                // Append the rest of the indent string
                this._compiledTemplate.append(indentString, i, indentString.length())
                return
            }

            // If the template char doesn't match the expected indent char
            if (this._compiledTemplate.charAt(lastNL + i) != indentString.charAt(i)) {
                // The template diverged from the indent
                if (isIndentChar(this._compiledTemplate.charAt(lastNL + i))) {
                    // It is not the expected indent but it is an indent at least so
                    // we will fix the indent if the rest of the template is also indents
                    for (int j = lastNL + i; j < this._compiledTemplate.length(); j++) {
                        // If we run into a non-indent char then the string must already be partially indented so
                        // we are done trying to indent
                        if (!isIndentChar(this._compiledTemplate.charAt(j)))
                            return
                    }

                    // If we made it here the rest of the template is indents but it is bad indents so
                    // just replace it with the tail of the indent string
                    this._compiledTemplate.setLength(lastNL + i)
                    this._compiledTemplate.append(indentString, i, indentString.length())
                    return
                }

                return
            }
        }

        // The beginning to the last line is the proper indent string but it may extend too long
        this._compiledTemplate.setLength(lastNL + indentString.length())
    }

    SimpleScriptGenerator putIndentedTemplateCode(String code) {
        this.putNecessaryIndent()
        compileInto(this._compiledTemplate, code)
        return this
    }

    SimpleScriptGenerator putTemplateCode(String code) {
        this.putNecessaryIndent()
        compileInto(this._compiledTemplate, code)
        return this
    }

    String detectIndent() {
        int lineStart = this._compiledTemplate.lastIndexOf('\n') + 1
        int indentEnd = lineStart

        while (indentEnd < this._compiledTemplate.length() && isIndentChar(this._compiledTemplate.charAt(indentEnd)))
            indentEnd++

        return this._compiledTemplate.substring(lineStart, indentEnd)
    }

    String replaceToken(int flags, String token) {
        String value = this.getTokenValue(token)

        if (value == null)
            throw new IllegalStateException("Unknown token '$token' in replacement")

        if ((flags & FLAG_RECURSIVE) > 0)
            return compile(value)

        if ((flags & FLAG_SMART) > 0) {
            String indent = detectIndent()
            return indentCode(value, indent)
        }

        return value
    }

    String compile(String template) {
        StringBuilder into = new StringBuilder()
        compileInto(into, template)
        return into.toString()
    }

    private void compileInto(StringBuilder into, String template) {
        Matcher tokenMatcher = tokenPattern.matcher(template)
        int pos = 0
        while (tokenMatcher.find()) {
            String flagString = tokenMatcher.group('flags') ?: null
            String token = tokenMatcher.group('token')

            int flags = 0
            if (flagString != null) {
                for (int i = 0; i < flagString.length(); i++) {
                    switch (flagString.charAt(i)) {
                        case 's' as char:
                            flags |= FLAG_SMART
                            break
                        case 'r' as char:
                            flags |= FLAG_RECURSIVE
                            break
                    }
                }
            }

            into.append(template, pos, tokenMatcher.start())
            pos = tokenMatcher.end()
            into.append(replaceToken(flags, token))
        }
        into.append(template, pos, template.length())
    }

    void write(OutputStream out) {
        String compiled = this._compiledTemplate.toString()
        out << new StringReader(compiled)
    }

    String getCompiled() {
        return this._compiledTemplate.toString()
    }

    // Operator overloads
    Object getAt(String name) {
        return this.getTokenValue(name)
    }

    void putAt(String name, Object value) {
        this.putToken(name, value)
    }

    SimpleScriptGenerator rightShift(OutputStream out) {
        this.write(out)
        return this
    }

    SimpleScriptGenerator leftShift(String code) {
        return this.putTemplateCode(code)
    }
}