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
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.ConfigureUtil

open class KernelExtension(private val project: Project) {
    private val _kernelName = project.objects.property(String::class.java).convention(project.name)
    private val _kernelDisplayName = project.objects.property(String::class.java).convention(_kernelName)
    private val _kernelLanguage = project.objects.property(String::class.java).convention(_kernelName)

    private val _kernelInterruptMode = project.objects.property(String::class.java).convention("message")

    private val _kernelEnv = project.objects.mapProperty(String::class.java, String::class.java).convention(mapOf())

    private val _kernelExecutable = project.objects.fileProperty().convention((project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar).archiveFile)

    private val _kernelResources = project.files(project.fileTree("kernel"))

    private val _kernelParameters = project.objects.property(KernelParameterSpecContainer::class.java).convention(KernelParameterSpecContainer(project))

    fun getKernelName(): String = this._kernelName.get()
    fun getKernelNameProvider(): Provider<String> = this._kernelName
    fun setKernelName(kernelName: String) = this._kernelName.set(kernelName)

    fun getKernelDisplayName(): String = this._kernelDisplayName.get()
    fun getKernelDisplayNameProvider(): Provider<String> = this._kernelDisplayName
    fun setKernelDisplayName(kernelDisplayName: String) = this._kernelDisplayName.set(kernelDisplayName)

    fun getKernelLanguage(): String = this._kernelLanguage.get()
    fun getKernelLanguageProvider(): Provider<String> = this._kernelLanguage
    fun setKernelLanguage(kernelLanguage: String) = this._kernelLanguage.set(kernelLanguage)

    fun getKernelInterruptMode(): String = this._kernelInterruptMode.get()
    fun getKernelInterruptModeProvider(): Provider<String> = this._kernelInterruptMode
    fun setKernelInterruptMode(kernelInterruptMode: String) = this._kernelInterruptMode.set(kernelInterruptMode)

    fun getKernelEnv(): Map<String, String> = this._kernelEnv.get()
    fun getKernelEnvProvider(): Provider<Map<String, String>> = this._kernelEnv
    fun setKernelEnv(kernelEnv: Map<String, String>) = this._kernelEnv.set(kernelEnv)
    fun kernelEnv(kernelEnv: Map<String, String>) = this._kernelEnv.putAll(kernelEnv)
    fun kernelEnv(configure: Action<in MutableMap<String, String>>) = this._kernelEnv.set(this.getKernelEnv().toMutableMap().also(configure::execute))

    fun getKernelExecutable(): RegularFile = this._kernelExecutable.get()
    fun getKernelExecutableProvider(): Provider<RegularFile> = this._kernelExecutable
    fun setKernelExecutable(file: RegularFile) = this._kernelExecutable.set(file)

    fun getKernelResources(): FileCollection = this._kernelResources
    fun setKernelResources(kernelResources: FileCollection) = this._kernelResources.setFrom(kernelResources)
    fun kernelResources(configure: Action<in CopySpec>) = this._kernelResources.setFrom(Copy().also(configure::execute).source)
    fun kernelResources(@DelegatesTo(value = CopySpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) {
        this._kernelResources.setFrom(ConfigureUtil.configure(configureClosure, Copy()).source)
    }

    fun getKernelParameters(): KernelParameterSpecContainer = this._kernelParameters.get()
    fun getKernelParametersProvider(): Provider<KernelParameterSpecContainer> = this._kernelParameters
    fun setKernelParameters(kernelParameters: KernelParameterSpecContainer) = this._kernelParameters.set(kernelParameters)
    fun kernelParameters(configure: Action<in KernelParameterSpecContainer>) = configure.execute(this.getKernelParameters())
    fun kernelParameters(@DelegatesTo(value = KernelParameterSpecContainer::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) {
        ConfigureUtil.configure(configureClosure, this.getKernelParameters())
    }
}