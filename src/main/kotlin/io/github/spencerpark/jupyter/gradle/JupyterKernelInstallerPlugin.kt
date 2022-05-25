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
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.GradleVersion

private val REQUIRED_GRADLE_VERSION = GradleVersion.version("6.0")

class JupyterKernelInstallerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (GradleVersion.current() < REQUIRED_GRADLE_VERSION)
            throw GradleException("The io.github.spencerpark.jupyter-kernel-installer plugin requires gradle >= ${REQUIRED_GRADLE_VERSION.version} but this project is using ${GradleVersion.current().version}")

        val kernelExtension = project.extensions.create("jupyter", KernelExtension::class.java, project)
        val configureInstallProps = Action { installSpec: KernelInstallSpec ->
            installSpec.kernelName.convention(kernelExtension.kernelName)
            installSpec.kernelDisplayName.convention(kernelExtension.kernelDisplayName)
            installSpec.kernelLanguage.convention(kernelExtension.kernelLanguage)

            installSpec.kernelInterruptMode.convention(kernelExtension.kernelInterruptMode)

            installSpec.kernelEnv.convention(kernelExtension.kernelEnv)

            installSpec.kernelExecutable.convention(kernelExtension.kernelExecutable)

            installSpec.setKernelResources(kernelExtension.kernelResources)
        }

        project.tasks.create(
            "installKernel",
            InstallKernelTask::class.java,
            InstallKernelTaskAction(kernelExtension, configureInstallProps)
        )

        project.tasks.create(
            "zipKernel",
            ZipKernelTask::class.java,
            ZipKernelTaskAction(kernelExtension, configureInstallProps)
        )

        // If the java plugin was already applied, hook up the executable as the default executable for
        // the extension.
        project.plugins.withType(JavaPlugin::class.java, ApplyJavaConvention(project, kernelExtension))
    }
}

private class ApplyJavaConvention(val project: Project, val kernelExtension: KernelExtension): Action<JavaPlugin>  {
    override fun execute(java: JavaPlugin) {
        kernelExtension.kernelExecutable.convention((project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar).archiveFile)

        project.tasks.withType(InstallKernelTask::class.java).configureEach {
            it.dependsOn(JavaPlugin.JAR_TASK_NAME)
        }
        project.tasks.withType(ZipKernelTask::class.java).configureEach {
            it.dependsOn(JavaPlugin.JAR_TASK_NAME)
        }
    }

}

private class Validate(val kernelInstallSpec: KernelInstallSpec): Action<Task> {
    override fun execute(task: Task) {
        kernelInstallSpec.validate()
    }

}

private class InstallKernelTaskAction(
    private val kernelExtension: KernelExtension,
    private val configureInstallProps: Action<KernelInstallSpec>
) : Action<InstallKernelTask> {
    override fun execute(task: InstallKernelTask) {
        task.description = "Locally install the kernel."
        task.group = "jupyter"

        // Note that this sets the values to **providers**. Essentially the providers act as
        // references to a value so that they are shared between tasks and configurations.
        task.kernelInstallSpec(configureInstallProps)
        task.doFirst(Validate(task.kernelInstallSpec))

        task.kernelParameters.params.convention(kernelExtension.kernelParameters.params)
    }

}

private class ZipKernelTaskAction(
    private val kernelExtension: KernelExtension,
    private val configureInstallProps: Action<KernelInstallSpec>
): Action<ZipKernelTask> {
    override fun execute(task: ZipKernelTask) {
        task.description = "Create a zip with the kernel files."
        task.group = "jupyter"

        task.kernelInstallSpec(configureInstallProps)
        task.doFirst(Validate(task.kernelInstallSpec))

        task.kernelParameters.params.convention(kernelExtension.kernelParameters.params)
    }

}

