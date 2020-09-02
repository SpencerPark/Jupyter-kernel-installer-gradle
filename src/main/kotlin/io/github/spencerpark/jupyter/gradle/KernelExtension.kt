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

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

open class KernelExtension @Inject constructor(project: Project, objects: ObjectFactory) : WithGradleDslExtensions {
    val kernelName: Property<String> = objects.property(String::class.java).convention(project.name)
    val kernelDisplayName: Property<String> = objects.property(String::class.java).convention(kernelName)
    val kernelLanguage: Property<String> = objects.property(String::class.java).convention(kernelName)

    val kernelInterruptMode: Property<String> = objects.property(String::class.java).convention("message")

    val kernelEnv: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(mutableMapOf())

    val kernelExecutable: RegularFileProperty = objects.fileProperty()//.convention((project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar).archiveFile)

    val kernelResources: ConfigurableFileCollection = project.files(project.fileTree("kernel"))

    val kernelParameters: KernelParameterSpecContainer = KernelParameterSpecContainer(objects)

    fun kernelEnv(kernelEnv: Map<String, String>) = this.kernelEnv.putAll(kernelEnv)
    fun kernelEnv(configure: Action<in MutableMap<String, String>>) = this.kernelEnv.set(this.kernelEnv.get().also(configure::execute))

    fun setKernelResources(kernelResources: FileCollection) = this.kernelResources.setFrom(kernelResources)
    fun kernelResources(configure: Action<in CopySpec>) = this.kernelResources.setFrom(Copy().also(configure::execute).source)
    fun kernelResources(@DelegatesTo(value = CopySpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) {
        this.kernelResources.setFrom(ConfigureUtil.configure(configureClosure, Copy()).source)
    }

    fun kernelParameters(configure: Action<in KernelParameterSpecContainer>) = configure.execute(this.kernelParameters)
    fun kernelParameters(@DelegatesTo(value = KernelParameterSpecContainer::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) {
        ConfigureUtil.configure(configureClosure, this.kernelParameters)
    }
}