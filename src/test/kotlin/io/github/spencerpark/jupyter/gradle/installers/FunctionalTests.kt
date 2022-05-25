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

import io.github.spencerpark.jupyter.gradle.assertTaskOutcome
import io.github.spencerpark.jupyter.gradle.withGroovyGradleProjectLayout
import io.github.spencerpark.jupyter.gradle.withKotlinGradleProjectLayout
import io.kotest.core.spec.style.StringSpec
import org.gradle.testkit.runner.TaskOutcome

class FunctionalTests : StringSpec({
    "builds a trivial groovy-dsl project" {
        withGroovyGradleProjectLayout {
            buildFile.appendText("""
                plugins {
                    id 'java'
                    id 'io.github.spencerpark.jupyter-kernel-installer'
                }
                
                zipKernel {
                    installers {
                        with 'python'
                    }
                }
            """.trimIndent())

            // TODO build this runner into the test block to run against multiple gradle versions to test compatibility
            assertTaskOutcome("zipKernel", TaskOutcome.SUCCESS)
            assertTaskOutcome("zipKernel", TaskOutcome.UP_TO_DATE)
        }
    }

    "builds a trivial kotlin-dsl project" {
        withKotlinGradleProjectLayout {
            buildFile.appendText("""
                import io.github.spencerpark.jupyter.gradle.ZipKernelTask

                plugins {
                    id("java")
                    id("io.github.spencerpark.jupyter-kernel-installer")
                }
                
                tasks.named<ZipKernelTask>("zipKernel") {
                    withInstaller("python")
                }
            """.trimIndent())

            // TODO build this runner into the test block to run against multiple gradle versions to test compatibility
            assertTaskOutcome("zipKernel", TaskOutcome.SUCCESS)
            assertTaskOutcome("zipKernel", TaskOutcome.UP_TO_DATE)
        }
    }
})
