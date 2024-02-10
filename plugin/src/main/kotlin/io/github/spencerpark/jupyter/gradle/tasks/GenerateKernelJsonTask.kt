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

package io.github.spencerpark.jupyter.gradle.tasks

import io.github.spencerpark.jupyter.gradle.KernelInstallSpec
import io.github.spencerpark.jupyter.gradle.KernelJson
import io.github.spencerpark.jupyter.gradle.WithGradleDslExtensions
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * A placeholder identifier that is added into the kernel.json to be replaced when installed. The
 * path to the kernel is not known until it is installed which is why this field needs to be
 * deferred.
 */
private const val UNSET_PATH_TOKEN = "@KERNEL_INSTALL_DIRECTORY@"

@CacheableTask
abstract class GenerateKernelJsonTask : DefaultTask(), WithGradleDslExtensions {

    @get:Nested
    abstract val kernelInstallSpec: KernelInstallSpec

    @get:OutputFile
    abstract val output: RegularFileProperty

    fun kernelInstallSpec(configure: Action<in KernelInstallSpec>): GenerateKernelJsonTask {
        configure.execute(kernelInstallSpec)
        return this
    }

    init {
        output.convention(project.layout.buildDirectory.file("jupyter/kernel.json"))
    }

    @TaskAction
    fun execute() {
        val spec = with(kernelInstallSpec) {
            KernelJson(
                "${UNSET_PATH_TOKEN}/${kernelExecutable.get().asFile.name}",
                kernelDisplayName.get(),
                kernelLanguage.get(),
                kernelInterruptMode.get(),
                kernelEnv.get(),
                kernelMetadata.get(),
            )
        }
        output.get().asFile.writeText(spec.toString(), Charsets.UTF_8)
    }
}