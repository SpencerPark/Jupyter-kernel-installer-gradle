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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

@CompileStatic
class JupyterKernelInstallerPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.with {
            KernelExtension kernelProps = extensions.create('jupyter', KernelExtension.class, project)
            Action<KernelInstallProperties> configureInstallProps = { KernelInstallProperties installProps ->
                installProps.setKernelName(kernelProps.getKernelNameProvider())
                installProps.setKernelDisplayName(kernelProps.getKernelDisplayNameProvider())
                installProps.setKernelLanguage(kernelProps.getKernelLanguageProvider())

                installProps.setKernelInterruptMode(kernelProps.getKernelInterruptModeProvider())

                installProps.setKernelEnv(kernelProps.getKernelEnvProvider())

                installProps.setKernelExecutable(kernelProps.getKernelExecutableProvider())

                installProps.setKernelResources(kernelProps.getKernelResources())
            }

            tasks.create('installKernel', InstallKernelTask.class, (Action<InstallKernelTask>) { InstallKernelTask task ->
                task.description = 'Locally install the kernel.'
                task.group = 'jupyter'
                task.dependsOn(JavaPlugin.JAR_TASK_NAME)

                task.kernelInstallProps(configureInstallProps)
                task.doFirst(task.kernelInstallProps.&validate)

                task.kernelInstallPath = kernelProps.kernelInstallPathProvider

            })

            tasks.create('zipKernel', ZipKernelTask.class, (Action<ZipKernelTask>) { ZipKernelTask task ->
                task.description = 'Create a zip with the kernel files.'
                task.group = 'jupyter'
                task.dependsOn(JavaPlugin.JAR_TASK_NAME)

                task.kernelInstallProps(configureInstallProps)
                task.doFirst(task.kernelInstallProps.&validate)
            })
        }
    }
}

