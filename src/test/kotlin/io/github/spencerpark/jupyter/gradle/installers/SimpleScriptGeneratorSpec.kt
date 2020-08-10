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

class SimpleScriptGeneratorSpec : StringSpec({
    lateinit var generator: SimpleScriptGenerator

    beforeTest {
        generator = SimpleScriptGenerator("    ", "\n")
    }

    "simple replacement" {
        generator["token"] = "replacement"

        generator.compile("String with a @token@").toString() shouldBe "String with a replacement"
    }

    "dynamic replacement closure" {
        generator["token"] = { this += "test" }

        generator.compile("@token@").toString() shouldBe "test"
    }

    "skips unknown" {
        generator.compile("String with an @unknown_token@").toString() shouldBe "String with an @unknown_token@"
    }

    "handles recursive tokens" {
        generator["token_top"] = "nested @token_bot@"
        generator["token_bot"] = "token"

        generator.compile("String with a @/r/token_top@ in it").toString() shouldBe "String with a nested token in it"
        generator.compile("String with a @token_top@ in it").toString() shouldBe "String with a nested @token_bot@ in it"
    }

    "builds a script" {
        generator["token"] = "replacement"

        generator.compile {
            this += "Sample line with a @token@ in it\n"
            this += "A second line"
        }.toString() shouldBe "Sample line with a replacement in it\nA second line"
    }

    "indents properly" {
        generator.compile {
            +"Sample line\n"
            indented {
                +"Indented once\n"
                indented {
                    +"Indented twice\n"
                }
                +"Indented once"
            }
        }.toString() shouldBe listOf(
                "Sample line",
                "    Indented once",
                "        Indented twice",
                "    Indented once"
        ).joinToString("\n")
    }

    "properly smart indents" {
        generator["token"] = "multiline\nreplacement"

        generator.compile("  Two spaces before the @/s/token@").toString() shouldBe listOf(
                "  Two spaces before the multiline",
                "  replacement"
        ).joinToString("\n")
    }

    "smart indent with indented token" {
        generator["token"] = {
            +"Simple line\n"
            indented {
                +"Indented line"
            }
        }

        generator.compile("    @/s/token@").toString() shouldBe listOf(
                "    Simple line",
                "        Indented line"
        ).joinToString("\n")
    }

    "fixes indent" {
        generator.compile {
            +"unindented\n  "
            +"should be unindented"
        }.toString() shouldBe listOf(
                "unindented",
                "should be unindented"
        ).joinToString("\n")
    }
})
