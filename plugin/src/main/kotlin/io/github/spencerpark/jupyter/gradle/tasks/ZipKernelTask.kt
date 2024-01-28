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
import io.github.spencerpark.jupyter.gradle.WithGradleDslExtensions
import io.github.spencerpark.jupyter.gradle.installers.InstallerMethod
import io.github.spencerpark.jupyter.gradle.installers.InstallersSpec
import org.gradle.api.Action
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.options.Option

@CacheableTask
abstract class ZipKernelTask : Zip(), WithGradleDslExtensions {
    companion object {
        /**
         * A placeholder identifier that is added into the kernel.json to be replaced when installed. The
         * path to the kernel is not known until it is installed which is why this field needs to be
         * deferred.
         */
        private const val UNSET_PATH_TOKEN = "@KERNEL_INSTALL_DIRECTORY@"
    }

    /**
     * The properties of the kernel being installed. These will end up inside the generated kernel.json.
     */
    @get:Nested
    abstract val kernelInstallSpec: KernelInstallSpec

    /**
     * A specification of which installers to generate and include in the output zip.
     */
    @get:Nested
    val installers = InstallersSpec()

    init {
        installers.with("python")

        // Kernel resources, executable, kernel.json go into a directory matching the kernel's name.
        super.with(project.copySpec { spec ->
            spec.into(kernelInstallSpec.kernelName) { intoKernelDir ->
                intoKernelDir.from(project.tasks.withType(GenerateKernelJsonTask::class.java).first().output)

                intoKernelDir.from(kernelInstallSpec.kernelResources)
                intoKernelDir.from(kernelInstallSpec.kernelExecutable)
            }
        })

        // The installer, if not removed, is put into the root.
        super.into("") { spec ->
            spec.from({
                if (InstallerMethod.PYTHON_SCRIPT in installers) {
                    return@from project.tasks.withType(GeneratePythonInstallerTask::class.java).first().output
                }

                return@from null
            })
        }
    }

    fun kernelInstallSpec(configure: Action<in KernelInstallSpec>): ZipKernelTask {
        configure.execute(kernelInstallSpec)
        return this
    }

    fun installers(configure: Action<in InstallersSpec>): ZipKernelTask {
        configure.execute(installers)
        return this
    }

    fun withInstaller(vararg methods: Any?): InstallersSpec {
        return installers.with(*methods)
    }

    fun withoutInstaller(vararg methods: Any?): InstallersSpec {
        return installers.without(*methods)
    }

    @Option(option = "with", description = "Include an installer in the zipped bundle.")
    fun withInstaller(installerName: List<String>) {
        installers.with(installerName)
    }

    @Option(option = "without", description = "Exclude an installer in the zipped bundle.")
    fun withoutInstaller(installerName: List<String>) {
        installers.without(installerName)
    }

    @Option(option = "archive-name", description = "Set the name of the output archive.")
    fun setArchiveFileName(name: String) {
        super.getArchiveFileName().set(name)
    }
}
