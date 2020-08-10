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

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.util.GradleVersion

private val REQUIRED_GRADLE_VERSION = GradleVersion.version("6.0")

class JupyterKernelInstallerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (GradleVersion.current() < REQUIRED_GRADLE_VERSION)
            throw GradleException("The io.github.spencerpark.jupyter-kernel-installer plugin requires gradle >= ${REQUIRED_GRADLE_VERSION.version} but this project is using ${GradleVersion.current().version}")

        with(project) {
            val kernelProps = extensions.create("jupyter", KernelExtension::class.java, project)
            val configureInstallProps = Action { installSpec: KernelInstallSpec ->
                installSpec.setKernelName(kernelProps.getKernelNameProvider())
                installSpec.setKernelDisplayName(kernelProps.getKernelDisplayNameProvider())
                installSpec.setKernelLanguage(kernelProps.getKernelLanguageProvider())

                installSpec.setKernelInterruptMode(kernelProps.getKernelInterruptModeProvider())

                installSpec.setKernelEnv(kernelProps.getKernelEnvProvider())

                installSpec.setKernelExecutable(kernelProps.getKernelExecutableProvider())

                installSpec.setKernelResources(kernelProps.getKernelResources())
            }

            tasks.create("installKernel", InstallKernelTask::class.java, Action { task: InstallKernelTask ->
                task.description = "Locally install the kernel."
                task.group = "jupyter"
                task.dependsOn(JavaPlugin.JAR_TASK_NAME)

                // Note that this sets the values to **providers**. Essentially the providers act as
                // references to a value so that they are shared between tasks and configurations.
                task.kernelInstallSpec(configureInstallProps)
                task.doFirst { task.getKernelInstallSpec().validate() }

                task.getKernelParameters().setParams(kernelProps.getKernelParameters().getParamsProvider())
            })

            tasks.create("zipKernel", ZipKernelTask::class.java, Action { task: ZipKernelTask ->
                task.description = "Create a zip with the kernel files."
                task.group = "jupyter"
                task.dependsOn(JavaPlugin.JAR_TASK_NAME)

                task.kernelInstallSpec(configureInstallProps)
                task.doFirst { task.kernelInstallSpec.validate() }

                task.kernelParameters.setParams(kernelProps.getKernelParameters().getParamsProvider())
            })
        }
    }
}

