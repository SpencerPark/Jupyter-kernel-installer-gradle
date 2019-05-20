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
package io.github.spencerpark.jupyter.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.util.GradleVersion

@CompileStatic
class JupyterKernelInstallerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        if (GradleVersion.current() < GradleVersion.version('5.0'))
            throw new GradleException("The io.github.spencerpark.jupyter-kernel-installer plugin requires gradle >= 4.6 but this project is using ${GradleVersion.current().version}")

        project.with {
            KernelExtension kernelProps = extensions.create('jupyter', KernelExtension.class, project)
            Action<KernelInstallSpec> configureInstallProps = { KernelInstallSpec installSpec ->
                installSpec.setKernelName(kernelProps.getKernelNameProvider())
                installSpec.setKernelDisplayName(kernelProps.getKernelDisplayNameProvider())
                installSpec.setKernelLanguage(kernelProps.getKernelLanguageProvider())

                installSpec.setKernelInterruptMode(kernelProps.getKernelInterruptModeProvider())

                installSpec.setKernelEnv(kernelProps.getKernelEnvProvider())

                installSpec.setKernelExecutable(kernelProps.getKernelExecutableProvider())

                installSpec.setKernelResources(kernelProps.getKernelResources())
            }

            tasks.create('installKernel', InstallKernelTask.class, (Action<InstallKernelTask>) { InstallKernelTask task ->
                task.description = 'Locally install the kernel.'
                task.group = 'jupyter'
                task.dependsOn(JavaPlugin.JAR_TASK_NAME)

                // Note that this sets the values to **providers**. Essentially the providers act as
                // references to a value so that they are shared between tasks and configurations.
                task.kernelInstallSpec(configureInstallProps)
                task.doFirst(task.kernelInstallSpec.&validate)

                task.kernelParameters.params = kernelProps.kernelParameters.paramsProvider
            })

            tasks.create('zipKernel', ZipKernelTask.class, (Action<ZipKernelTask>) { ZipKernelTask task ->
                task.description = 'Create a zip with the kernel files.'
                task.group = 'jupyter'
                task.dependsOn(JavaPlugin.JAR_TASK_NAME)

                task.kernelInstallSpec(configureInstallProps)
                task.doFirst(task.kernelInstallSpec.&validate)

                task.kernelParameters.params = kernelProps.kernelParameters.paramsProvider
            })
        }
    }
}

