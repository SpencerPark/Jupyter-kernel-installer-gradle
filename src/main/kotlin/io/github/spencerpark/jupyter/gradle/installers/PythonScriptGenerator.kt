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

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

class PythonScriptGenerator() : SimpleScriptGenerator("    ", "\n") {
    var parserVariableName: String = "parser"
    var argsVariableName: String = "args"
    var jsonParserVariableName: String = "json"
    var parsedEnvFieldName: String = "env"

    private val params = mutableListOf<InstallerParameterSpec>()

    init {
        putToken("GENERATED_ARGS") {
            params.forEach { writeParameterParser(it) }
        }

        putToken("GENERATED_DEFAULT_REPLACEMENT") {
            this += "if not hasattr($argsVariableName, ${literal(parsedEnvFieldName)})"
            this += " or getattr($argsVariableName, ${literal(parsedEnvFieldName)}) is None:\n"
            this.indented {
                this += "setattr($argsVariableName, ${literal(parsedEnvFieldName)}, {})\n"
            }

            params.forEach { param ->
                if (param.defaultValue != null) {
                    this += "getattr($argsVariableName, ${literal(parsedEnvFieldName)})"
                    this += ".setdefault(${literal(param.envVar)}, ${literal(compile(param.defaultValue!!).toString())})\n"
                }
            }
        }
        putToken("ARG_ALIASES_DICT") {
            params.forEach { appendAliases(it) }
        }
        putToken("NAME_MAP_DICT") {
            params.forEach { appendNameMap(it) }
        }
    }

    fun addParameter(spec: InstallerParameterSpec) {
        this.params.add(spec)
    }

    private val pyJsonString = Json(JsonConfiguration.Stable);
    private fun literal(value: String) = pyJsonString.stringify(String.serializer(), value)

    private fun Document.writeParameterParser(spec: InstallerParameterSpec) {
        this += "$parserVariableName.add_argument(\n"
        this.indented {
            this += literal("--${spec.name}") + ",\n"
            this += "dest=${literal(parsedEnvFieldName)},\n"
            this += "action=EnvVar,\n"
            this += "aliases=ALIASES,\n" // TODO these names should probably be configurable to remain consistent
            this += "name_map=NAME_MAP,\n"

            if (spec.description != null) {
                this += "help=${literal(compile(spec.description!!).toString())},\n"
            }

            this += "type=type_assertion(${literal(spec.name)}, "
            when (spec.type) {
                InstallerParameterSpec.Type.FLOAT -> this += "float),\n"
                else -> this += "str),\n"
            }

            if (spec.listSep != null) {
                this += "list_sep="
                if (spec.listSep == InstallerParameterSpec.FILE_SEPARATOR)
                    this += "os.sep,\n"
                else if (spec.listSep == InstallerParameterSpec.PATH_SEPARATOR)
                    this += "os.pathsep,\n"
                else
                    this += "${literal(spec.listSep!!)},\n"
            }

            if (spec.choices != null && spec.choices!!.isNotEmpty()) {
                this += "choices=["
                spec.choices!!.forEach { this += "${literal(it)}, " }
                this += "],\n"
            }
        }
        this += ")\n"
    }

    private fun Document.appendAliases(spec: InstallerParameterSpec) {
        if (spec.aliases != null) {
            this += "${literal(spec.envVar)}: {\n"
            this.indented {
                spec.aliases!!.forEach {
                    this += "${literal(it.key)}: ${literal(it.value)},\n"
                }
            }
            this += "},\n"
        }
    }

    private fun Document.appendNameMap(spec: InstallerParameterSpec) {
        this += "${literal(spec.name)}: ${literal(spec.envVar)},\n"
    }
}
