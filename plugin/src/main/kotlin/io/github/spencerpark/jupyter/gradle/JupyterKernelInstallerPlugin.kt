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

import io.github.spencerpark.jupyter.gradle.tasks.GenerateKernelJsonTask
import io.github.spencerpark.jupyter.gradle.tasks.GeneratePythonInstallerTask
import io.github.spencerpark.jupyter.gradle.tasks.InstallKernelTask
import io.github.spencerpark.jupyter.gradle.tasks.ZipKernelTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.GradleVersion

private val REQUIRED_GRADLE_VERSION = GradleVersion.version("8.0")

class JupyterKernelInstallerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (GradleVersion.current() < REQUIRED_GRADLE_VERSION) {
            project.logger.warn("The io.github.spencerpark.jupyter-kernel-installer plugin was build targeting gradle ${REQUIRED_GRADLE_VERSION.version} but this project is using ${GradleVersion.current().version}")
        }

        val kernelExtension = project.extensions.create(PROJECT_EXTENSION_NAME, KernelExtension::class.java, project)
        val wireSharedKernelInstallSpecConvention = Action { installSpec: KernelInstallSpec ->
            installSpec.kernelName.convention(kernelExtension.kernelName)
            installSpec.kernelDisplayName.convention(kernelExtension.kernelDisplayName)
            installSpec.kernelLanguage.convention(kernelExtension.kernelLanguage)

            installSpec.kernelInterruptMode.convention(kernelExtension.kernelInterruptMode)

            installSpec.kernelEnv.convention(kernelExtension.kernelEnv)

            installSpec.kernelMetadata.convention(kernelExtension.kernelMetadata)

            installSpec.kernelExecutable.convention(kernelExtension.kernelExecutable)

            installSpec.kernelResources.setFrom(kernelExtension.kernelResources)
        }

        project.tasks.register(GENERATE_KERNEL_JSON_TASK_NAME, GenerateKernelJsonTask::class.java) { task ->
            task.description = "Generate the kernel.json file for the kernel."
            task.group = TASK_GROUP_NAME

            task.kernelInstallSpec(wireSharedKernelInstallSpecConvention)
            task.doFirst { task.kernelInstallSpec.validate() }
        }

        project.tasks.register(GENERATE_PYTHON_INSTALLER_TASK_NAME, GeneratePythonInstallerTask::class.java) { task ->
            task.description = "Generate the python install script for the kernel."
            task.group = TASK_GROUP_NAME

            task.kernelName.convention(kernelExtension.kernelName)
            task.kernelParameters.params.convention(kernelExtension.kernelParameters.params)
        }

        // Unless a copyspec is configured for the kernel resources, this task is unused.
        project.tasks.register(STAGE_EXTRA_KERNEL_RESOURCES_TASK_NAME, Sync::class.java) { task ->
            task.description = "Stage kernel resources that need to be restructured."
            task.group = TASK_GROUP_NAME

            task.into(project.layout.buildDirectory.dir("${EXTENSION_BUILD_DIR_PREFIX}/extra-resources"))
        }

        project.tasks.register(INSTALL_KERNEL_TASK_NAME, InstallKernelTask::class.java) { task ->
            task.description = "Locally install the kernel."
            task.group = TASK_GROUP_NAME

            task.kernelInstallSpec(wireSharedKernelInstallSpecConvention)
            task.doFirst { task.kernelInstallSpec.validate() }

            task.kernelParameters.params.convention(kernelExtension.kernelParameters.params)
        }

        project.tasks.register(ZIP_KERNEL_TASK_NAME, ZipKernelTask::class.java) { task ->
            task.description = "Create a zip with the kernel files."
            task.group = TASK_GROUP_NAME

            task.kernelInstallSpec(wireSharedKernelInstallSpecConvention)
            task.doFirst { task.kernelInstallSpec.validate() }
        }

        // If the java plugin was already applied, hook up the executable as the default executable for
        // the extension.
        project.plugins.withType(JavaPlugin::class.java, ApplyJavaConvention(project, kernelExtension))
    }
}

private class ApplyJavaConvention(val project: Project, val kernelExtension: KernelExtension) : Action<JavaPlugin> {
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

