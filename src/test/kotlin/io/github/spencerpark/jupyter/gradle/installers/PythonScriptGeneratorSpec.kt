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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PythonScriptGeneratorSpec : StringSpec({
    lateinit var generator: PythonScriptGenerator

    lateinit var strParam: InstallerParameterSpec
    lateinit var floatParam: InstallerParameterSpec
    lateinit var aliasParam: InstallerParameterSpec
    lateinit var choiceParam: InstallerParameterSpec
    lateinit var listParam: InstallerParameterSpec
    lateinit var fileListParam: InstallerParameterSpec
    lateinit var descParam: InstallerParameterSpec
    lateinit var defaultParam: InstallerParameterSpec

    beforeTest {
        generator = PythonScriptGenerator()

        strParam = InstallerParameterSpec("param-1", "ENV_1")

        floatParam = InstallerParameterSpec("param-2", "ENV_2")
        floatParam.type = InstallerParameterSpec.Type.FLOAT

        aliasParam = InstallerParameterSpec("param-3", "ENV_3")
        aliasParam.type = InstallerParameterSpec.Type.FLOAT
        aliasParam.addAlias("ON", "1")
        aliasParam.addAlias("OFF", "0")

        choiceParam = InstallerParameterSpec("param-4", "ENV_4")
        choiceParam.choices = mutableListOf("a", "b", "c")

        listParam = InstallerParameterSpec("param-5", "ENV_5")
        listParam.listSep = " "

        fileListParam = InstallerParameterSpec("param-6", "ENV_6")
        fileListParam.useFileSeparator()

        descParam = InstallerParameterSpec("param-7", "ENV_7")
        descParam.description = """
            |A "multiline"
            |'description'
            |
        """.trimMargin("|")

        defaultParam = InstallerParameterSpec("param-8", "ENV_8")
        defaultParam.defaultValue = "DEFAULT"
    }

    fun addAllParams() {
        generator.addParameter(strParam)
        generator.addParameter(floatParam)
        generator.addParameter(aliasParam)
        generator.addParameter(choiceParam)
        generator.addParameter(listParam)
        generator.addParameter(fileListParam)
        generator.addParameter(descParam)
        generator.addParameter(defaultParam)
    }

    "properly generates an alias dictionary body" {
        addAllParams()

        generator.compile("@ARG_ALIASES_DICT@").toString() shouldBe """
            |"ENV_3": {
            |    "ON": "1",
            |    "OFF": "0",
            |},
            |
        """.trimMargin("|")
    }

    "properly generates a name map body" {
        addAllParams()

        generator.compile("    @/s/NAME_MAP_DICT@").toString() shouldBe """
            |    "param-1": "ENV_1",
            |    "param-2": "ENV_2",
            |    "param-3": "ENV_3",
            |    "param-4": "ENV_4",
            |    "param-5": "ENV_5",
            |    "param-6": "ENV_6",
            |    "param-7": "ENV_7",
            |    "param-8": "ENV_8",
            |    
        """.trimMargin("|")
    }

    "properly generates default value replacements" {
        addAllParams()

        generator.compile("@GENERATED_DEFAULT_REPLACEMENT@").toString() shouldBe """
            |if not hasattr(args, "env") or getattr(args, "env") is None:
            |    setattr(args, "env", {})
            |getattr(args, "env").setdefault("ENV_8", "DEFAULT")
            |
        """.trimMargin("|")
    }

    fun argumentCode(name: String, opts: Map<String, String>): String {
        val lines = mutableListOf(
                "parser.add_argument(",
                "    \"--$name\",",
                "    dest=\"env\",",
                "    action=EnvVar,",
                "    aliases=ALIASES,",
                "    name_map=NAME_MAP,"
        )

        opts.forEach { (key, value) ->
            lines.add("    $key=$value,")
        }

        lines.add(")")
        lines.add("")

        return lines.joinToString("\n")
    }

    "properly generates argument parser for str" {
        generator.addParameter(strParam)

        generator.compile("@GENERATED_ARGS@").toString() shouldBe
                argumentCode(strParam.name, mapOf("type" to """type_assertion("param-1", str)"""))
    }

    "properly generates argument parser for float" {
        generator.addParameter(floatParam)

        generator.compile("@GENERATED_ARGS@").toString() shouldBe
                argumentCode(floatParam.name, mapOf("type" to """type_assertion("param-2", float)"""))
    }

    "properly generates argument parser for alias" {
        generator.addParameter(aliasParam)

        generator.compile("@GENERATED_ARGS@").toString() shouldBe
                argumentCode(aliasParam.name, mapOf("type" to """type_assertion("param-3", float)"""))
    }

    "properly generates argument parser for choice" {
        generator.addParameter(choiceParam)

        generator.compile("@GENERATED_ARGS@").toString() shouldBe
                argumentCode(choiceParam.name, mapOf(
                        "type" to """type_assertion("param-4", str)""",
                        "choices" to """["a", "b", "c", ]"""
                ))
    }

    "properly generates argument parser for list" {
        generator.addParameter(listParam)

        generator.compile("@GENERATED_ARGS@").toString() shouldBe
                argumentCode(listParam.name, mapOf(
                        "type" to """type_assertion("param-5", str)""",
                        "list_sep" to """" """"
                ))
    }

    "properly generates argument parser for file list" {
        generator.addParameter(fileListParam)

        generator.compile("@GENERATED_ARGS@").toString() shouldBe
                argumentCode(fileListParam.name, mapOf(
                        "type" to """type_assertion("param-6", str)""",
                        "list_sep" to "os.sep"
                ))
    }

    "properly generates argument parser for desc" {
        generator.addParameter(descParam)

        generator.compile("@GENERATED_ARGS@").toString() shouldBe
                argumentCode(descParam.name, mapOf(
                        "help" to """"A \"multiline\"\n'description'\n"""",
                        "type" to """type_assertion("param-7", str)"""
                ))
    }

    "properly generates argument parser for default" {
        generator.addParameter(defaultParam)

        generator.compile("@GENERATED_ARGS@").toString() shouldBe
                argumentCode(defaultParam.name, mapOf("type" to """type_assertion("param-8", str)"""))
    }
})
