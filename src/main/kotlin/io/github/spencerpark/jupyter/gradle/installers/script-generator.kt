/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Spencer Park
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.github.spencerpark.jupyter.gradle.installers

import java.io.OutputStream

internal sealed class TokenValue
internal data class StaticTokenValue(val value: Any) : TokenValue()
internal data class DynamicTokenValue(val value: Document.() -> Unit) : TokenValue()

interface Document {
    fun indent()
    fun dedent()
    fun resetIndent()

    fun indented(block: Document.() -> Unit) {
        indent()
        this.block()
        dedent()
    }

    fun append(value: String)

    operator fun plusAssign(value: String) {
        this.append(value)
    }

    operator fun String.unaryPlus() {
        this@Document.append(this)
    }

    fun write(out: OutputStream)

    fun rightShift(out: OutputStream) {
        this.write(out)
    }

    override fun toString(): String
}

private class SmartStringBuilder(
        val compiled: StringBuilder = StringBuilder(),
        private var indent: Int = 0,
        private val indentString: String = "    ",
        private val newLine: String = System.lineSeparator()
) : Document {
    init {
        for (i in indentString.indices) {
            if (!isIndentChar(indentString[i]))
                throw IllegalArgumentException("Indent string must only contain spaces or tabs but found ${indentString[i]}")
        }
    }

    companion object {
        fun isIndentChar(c: Char) = when (c) {
            ' ', '\t' -> true
            else -> false
        }
    }

    override fun indent() {
        indent++
    }

    override fun dedent() {
        if (indent > 0)
            indent--
    }

    override fun resetIndent() {
        indent = 0
    }

    internal fun putIndent() {
        for (i in 0..indent)
            compiled.append(indentString)
    }

    // Put what ever is missing from the current indent so it is consistent with what the indent should be.
    private fun fixCurrentIndent() {
        val lastNL = compiled.lastIndexOf('\n') + 1

        // Return early if there is a non-indent char before the end of the string signalling
        // that this line is partially written
        for (i in lastNL until compiled.length) {
            if (!isIndentChar(compiled[i]))
                return
        }

        val linePrefix = indentString.repeat(indent)

        // Replace the necessary indent
        for (i in linePrefix.indices) {
            // If the current template index is past the end
            if ((lastNL + i) >= compiled.length) {
                // Append the rest of the indent string
                compiled.append(linePrefix, i, linePrefix.length)
                return
            }

            // If the template char doesn't match the expected indent char
            if (compiled[lastNL + i] != linePrefix[i]) {
                // The template diverged from the indent
                if (isIndentChar(compiled[lastNL + i])) {
                    // It is not the expected indent but it is an indent at least so
                    // we will fix the indent if the rest of the template is also indents
                    for (j in lastNL + i until compiled.length) {
                        // If we run into a non-indent char then the string must already be partially indented so
                        // we are done trying to indent
                        if (!isIndentChar(compiled[j]))
                            return
                    }

                    // If we made it here the rest of the template is indents but it is bad indents so
                    // just replace it with the tail of the indent string
                    compiled.setLength(lastNL + i)
                    compiled.append(linePrefix, i, linePrefix.length)
                    return
                }

                return
            }
        }

        // The beginning to the last line is the proper indent string but it may extend too long
        compiled.setLength(lastNL + linePrefix.length)
    }

    internal fun detectIndent(): String {
        val lineStart = compiled.lastIndexOf('\n') + 1
        var indentEnd = lineStart

        while (indentEnd < compiled.length && isIndentChar(compiled[indentEnd]))
            indentEnd++

        return compiled.substring(lineStart, indentEnd)
    }

    override fun append(value: String) {
        this.fixCurrentIndent()
        this.compiled.append(value)
    }

    override fun write(out: OutputStream) {
        compiled.toString().byteInputStream(Charsets.UTF_8).copyTo(out)
    }

    override fun toString(): String = compiled.toString()
}

class ScriptGeneratorDocument(private val document: Document, private val generator: SimpleScriptGenerator) : Document {
    override fun indent() = document.indent()
    override fun dedent() = document.dedent()
    override fun resetIndent() = document.resetIndent()
    override fun write(out: OutputStream) = document.write(out)
    override fun toString(): String = document.toString()

    // Can't use delegate `by document` because of the default overloads that need to invoke this.
    override fun append(value: String) {
        document.append(generator.compile(value).toString())
    }
}

@Suppress("UNCHECKED_CAST")
open class SimpleScriptGenerator(private val indentString: String = "    ", private val newLine: String = System.lineSeparator()) {
    private val tokens = mutableMapOf<String, TokenValue>()

    // A cached pattern that is null if not yet baked or the tokens change
    private var _tokenPattern: Regex? = null

    init {
        for (i in indentString.indices) {
            if (!isIndentChar(indentString[i]))
                throw IllegalArgumentException("Indent string must only contain spaces or tabs but found ${indentString[i]}")
        }
    }

    companion object {
        val VALID_TOKEN_NAME = Regex("^[a-zA-Z0-9_-]+$")

        const val FLAG_SMART = 1 shl 1
        const val FLAG_RECURSIVE = 1 shl 2

        fun indentCode(code: String, indent: String) = code.replace("\n", "\n$indent")

        fun isIndentChar(c: Char) = when (c) {
            ' ', '\t' -> true
            else -> false
        }
    }

    private val tokenPattern
        get(): Regex {
            if (_tokenPattern == null) {
                val allTokens = this.tokens.keys.joinToString("|")
                _tokenPattern = Regex("@(/(?<flags>[sr]+)/)?(?<token>$allTokens)@")
            }
            return _tokenPattern!!
        }

    private fun getTokenValue(token: String) =
            when (val value = this.tokens[token]) {
                null -> null
                is StaticTokenValue -> value.value.toString()
                is DynamicTokenValue -> {
                    val ssb = SmartStringBuilder()
                    value.value(ssb)
                    ssb.compiled.toString()
                }
            }

    private fun putToken(name: String, value: TokenValue) {
        if (!VALID_TOKEN_NAME.matches(name))
            throw IllegalArgumentException("Cannot use '$name' as a token name")

        val replaced = tokens.put(name, value)
        if (replaced == null) // This is a new token
            _tokenPattern = null
    }

    fun putToken(name: String, value: Document.() -> Unit) {
        this.putToken(name, DynamicTokenValue(value))
    }

    fun putToken(name: String, value: Any) {
        this.putToken(name, StaticTokenValue(value))
    }

    fun putTokens(tokens: Map<String, Any>) {
        tokens.entries.forEach {
            when (it.value) {
                is Function<*> -> this.putToken(it.key, it.value as (Document.() -> Unit))
                else -> this.putToken(it.key, it.value)
            }
        }
    }

    private fun replaceToken(sb: SmartStringBuilder, flags: Int, token: String): String {
        val value = this.getTokenValue(token) ?: throw IllegalStateException("Unknown token '$token' in replacement")

        if ((flags and FLAG_RECURSIVE) > 0)
            return compile(value).toString()

        if ((flags and FLAG_SMART) > 0) {
            val indent = sb.detectIndent()
            return indentCode(value, indent)
        }

        return value
    }

    fun compile(template: String): Document {
        val into = SmartStringBuilder()
        compileInto(into, template)
        return into
    }

    fun compile(generator: Document.() -> Unit): Document {
        val document = ScriptGeneratorDocument(SmartStringBuilder(), this)
        document.generator()
        return document
    }

    private fun compileInto(into: SmartStringBuilder, template: String) {
        val tokenMatcher = tokenPattern.toPattern().matcher(template)
        var pos = 0
        while (tokenMatcher.find()) {
            val flagString = tokenMatcher.group("flags")?.ifEmpty { null }
            val token = tokenMatcher.group("token")

            var flags = 0
            if (flagString != null) {
                for (i in flagString.indices) {
                    when (flagString[i]) {
                        's' -> flags = flags or FLAG_SMART
                        'r' -> flags = flags or FLAG_RECURSIVE
                    }
                }
            }

            into.compiled.append(template, pos, tokenMatcher.start())
            pos = tokenMatcher.end()
            into.compiled.append(replaceToken(into, flags, token))
        }
        into.compiled.append(template, pos, template.length)
    }

    // Operator overloads
    operator fun get(name: String): String? = this.getTokenValue(name)

    fun getAt(name: String): String? = this.getTokenValue(name)

    operator fun set(name: String, value: Document.() -> Unit) = this.putToken(name, value)
    operator fun set(name: String, value: Any) = this.putToken(name, value)

    fun putAt(name: String, value: Document.() -> Unit) = this.putToken(name, value)
    fun putAt(name: String, value: Any) = this.putToken(name, value)
}