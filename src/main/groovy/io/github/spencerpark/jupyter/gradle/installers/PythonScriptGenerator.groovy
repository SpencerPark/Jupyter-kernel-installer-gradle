/*
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

import groovy.json.JsonOutput
import groovy.json.StringEscapeUtils

class PythonScriptGenerator extends SimpleScriptGenerator {
    private String _parserVariableName = 'parser'
    private String _argsVariableName = 'args'
    private String _jsonParserVariableName = 'json'
    private String _parsedEnvFieldName = 'env'

    private List<InstallerParameterSpec> params

    PythonScriptGenerator() {
        super('    ', '\n')
        this.params = []
        super['GENERATED_ARGS'] = { PythonScriptGenerator out ->
            params.each(out.&writeParameterParser)
        }
        super['GENERATED_DEFAULT_REPLACEMENT'] = { PythonScriptGenerator out ->
            out << "if not hasattr($out.argsVariableName, "
            out.writeSafeStringLiteral(out.parsedEnvFieldName)
            out << ") or getattr($out.argsVariableName, "
            out.writeSafeStringLiteral(out.parsedEnvFieldName)
            out << ") is None:\n"
            out.indented {
                out << "setattr($out.argsVariableName, "
                out.writeSafeStringLiteral(out.parsedEnvFieldName)
                out << ', {})\n'
            }

            params.each { InstallerParameterSpec param ->
                if (param.defaultValue != null) {
                    out << "getattr($out.argsVariableName, '$out.parsedEnvFieldName').setdefault("
                    out.writeSafeStringLiteral(param.envVar)
                    out << ', '
                    out.writeSafeStringLiteral(param.defaultValue)
                    out << ')\n'
                }
            }
        }
        super['ARG_ALIASES_DICT'] = { PythonScriptGenerator out ->
            params.each(out.&writeAliases)
        }
        super['NAME_MAP_DICT'] = { PythonScriptGenerator out ->
            params.each(out.&writeNameMap)
        }
    }

    private PythonScriptGenerator(PythonScriptGenerator config) {
        super(config)
        this._parserVariableName = config._parserVariableName
        this._argsVariableName = config._argsVariableName
        this._jsonParserVariableName = config._jsonParserVariableName
        this._parsedEnvFieldName = config._parsedEnvFieldName
    }

    @Override
    protected SimpleScriptGenerator createDynamicTokenGenerator() {
        return new PythonScriptGenerator(this)
    }

    String getParserVariableName() {
        return this._parserVariableName
    }

    void setParserVariableName(String name) {
        this._parserVariableName = name
    }

    String getArgsVariableName() {
        return _argsVariableName
    }

    void setArgsVariableName(String name) {
        this._argsVariableName = name
    }

    String getJsonParserVariableName() {
        return this._jsonParserVariableName
    }

    void setJsonParserVariableName(String name) {
        this._jsonParserVariableName = name
    }

    String getParsedEnvFieldName() {
        return _parsedEnvFieldName
    }

    void setParsedEnvFieldName(String name) {
        this._parsedEnvFieldName = name
    }

    PythonScriptGenerator addParameter(InstallerParameterSpec paramSpec) {
        this.params.add(paramSpec)
        return this
    }

    private void writeSafeStringLiteral(String s) {
        this << "'${StringEscapeUtils.escapeJava(s)}'"
    }

    private void writeParameterParser(InstallerParameterSpec parameterSpec) {
        this << this.parserVariableName
        this << '.add_argument(\n'
        this.indented {
            this.writeSafeStringLiteral("--$parameterSpec.name")
            this << ',\n'
            this << "dest='$parsedEnvFieldName',\n"
            this << 'action=EnvVar,\n'
            this << 'aliases=ALIASES,\n' // TODO these names should probably be configurable to remain consistent
            this << 'name_map=NAME_MAP,\n'

            if (parameterSpec.description != null) {
                this << 'help='
                this.writeSafeStringLiteral(parameterSpec.description)
                this << ',\n'
            }

            this << "type=type_assertion('$parameterSpec.name', "
            switch (parameterSpec.type) {
                case InstallerParameterSpec.Type.FLOAT:
                    this << 'float),\n'
                    break
                default:
                    this << 'str),\n'
                    break
            }

            if (parameterSpec.listSep != null) {
                this << 'list_sep='
                if (parameterSpec.listSep == InstallerParameterSpec.FILE_SEPARATOR)
                    this << 'os.sep,\n'
                else if (parameterSpec.listSep == InstallerParameterSpec.PATH_SEPARATOR)
                    this << 'os.pathsep,\n'
                else {
                    this.writeSafeStringLiteral(parameterSpec.listSep)
                    this << ',\n'
                }
            }

            if (parameterSpec.choices != null && !parameterSpec.choices.isEmpty()) {
                this << 'choices=['
                parameterSpec.choices.each { String value ->
                    this.writeSafeStringLiteral(value)
                    this << ', '
                }
                this << '],\n'
            }
        }
        this << ')\n'
    }

    private void writeAliases(InstallerParameterSpec parameterSpec) {
        if (parameterSpec.aliases != null) {
            this.writeSafeStringLiteral(parameterSpec.envVar)
            this << ': {\n'
            this.indented {
                parameterSpec.aliases.each { k, v ->
                    this.writeSafeStringLiteral(k)
                    this << ': '
                    this.writeSafeStringLiteral(v)
                    this << ',\n'
                }
            }
            this << '},\n'
        }
    }

    private void writeNameMap(InstallerParameterSpec parameterSpec) {
        this.writeSafeStringLiteral(parameterSpec.name)
        this << ': '
        this.writeSafeStringLiteral(parameterSpec.envVar)
        this << ',\n'
    }
}
