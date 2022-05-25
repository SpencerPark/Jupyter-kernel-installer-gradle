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
package io.github.spencerpark.jupyter.gradle

import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

fun <T> withTempFolder(prefix: String = "tmp", suffix: String? = null, directory: File? = null, block: (folder: File) -> T): T {
    val folder = createTempDir(prefix, suffix, directory)

    try {
        return block(folder)
    } finally {
        folder.deleteRecursively()
    }
}

class GradleProjectLayout(
        val projectRoot: File,
        val buildFile: File
)

fun withGroovyGradleProjectLayout(block: GradleProjectLayout.() -> Unit) {
    withTempFolder(prefix = "gradle-test", suffix = "groovy") { projectRoot ->
        val buildFile = File(projectRoot, "build.gradle")
        buildFile.createNewFile()
        GradleProjectLayout(projectRoot, buildFile).block()
    }
}

fun withKotlinGradleProjectLayout(block: GradleProjectLayout.() -> Unit) {
    withTempFolder(prefix = "gradle-test", suffix = "kotlin") { projectRoot ->
        val buildFile = File(projectRoot, "build.gradle.kts")
        buildFile.createNewFile()
        GradleProjectLayout(projectRoot, buildFile).block()
    }
}

fun GradleProjectLayout.runTask(task: String): BuildResult = GradleRunner.create()
    .withProjectDir(projectRoot)
    .withPluginClasspath()
    .withArguments(task)
    .build()

fun GradleProjectLayout.assertTaskOutcome(task: String, outcome: TaskOutcome) {
    val result = runTask(task)
    result.task(":$task")?.outcome shouldBe outcome
}