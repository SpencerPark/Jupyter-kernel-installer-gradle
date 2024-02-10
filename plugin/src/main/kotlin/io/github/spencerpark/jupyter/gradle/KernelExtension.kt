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
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

open class KernelExtension @Inject constructor(private val project: Project, objects: ObjectFactory) : WithGradleDslExtensions {
    val kernelName: Property<String> = objects.property(String::class.java).convention(project.name)
    val kernelDisplayName: Property<String> = objects.property(String::class.java).convention(kernelName)
    val kernelLanguage: Property<String> = objects.property(String::class.java).convention(kernelName)

    val kernelInterruptMode: Property<String> = objects.property(String::class.java).convention("message")

    val kernelEnv: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(mutableMapOf())

    val kernelExecutable: RegularFileProperty = objects.fileProperty()

    val kernelResources: ConfigurableFileCollection = project.files(project.fileTree("kernel"))

    val kernelParameters: KernelParameterSpecContainer = objects.newInstance(KernelParameterSpecContainer::class.java)

    fun kernelName(kernelName: String?) = this.kernelName.set(kernelName)
    fun kernelDisplayName(kernelDisplayName: String?) = this.kernelDisplayName.set(kernelDisplayName)
    fun kernelLanguage(kernelLanguage: String?) = this.kernelLanguage.set(kernelLanguage)

    fun kernelInterruptMode(kernelInterruptMode: String?) = this.kernelInterruptMode.set(kernelInterruptMode)

    fun kernelEnv(kernelEnv: Map<String, String>) = this.kernelEnv.putAll(kernelEnv)
    fun kernelEnv(configure: Action<in MutableMap<String, String>>) = this.kernelEnv.set(this.kernelEnv.get().also(configure::execute))

    fun kernelExecutable(kernelExecutable: RegularFile) = this.kernelExecutable.set(kernelExecutable)
    fun kernelExecutable(kernelExecutable: Any) = this.kernelExecutable.set(project.file(kernelExecutable))
    fun setKernelExecutable(kernelExecutable: RegularFile) = this.kernelExecutable.set(kernelExecutable)

    fun kernelResources(configure: Action<in CopySourceSpec>) {
        val provider = project.tasks.named(STAGE_EXTRA_KERNEL_RESOURCES_TASK_NAME, AbstractCopyTask::class.java) { task ->
            val spec = project.copySpec()
            configure.execute(spec)
            task.with(spec)
        }
        kernelResources(provider)
    }

    fun <T : Task?> kernelResources(source: TaskProvider<T>) = this.kernelResources.setFrom(source)

    fun setKernelResources(kernelResources: FileCollection) = this.kernelResources.setFrom(kernelResources)

    fun kernelParameters(configure: Action<in KernelParameterSpecContainer>) = configure.execute(this.kernelParameters)
}