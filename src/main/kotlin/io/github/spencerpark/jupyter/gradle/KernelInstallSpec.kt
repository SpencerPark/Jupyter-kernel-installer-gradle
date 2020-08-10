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

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles

class KernelInstallSpec(project: Project) {
    private val _kernelName = project.objects.property(String::class.java)
    private val _kernelDisplayName = project.objects.property(String::class.java)
    private val _kernelLanguage = project.objects.property(String::class.java)

    private val _kernelInterruptMode = project.objects.property(String::class.java)

    private val _kernelEnv = project.objects.mapProperty(String::class.java, String::class.java)

    private val _kernelExecutable = project.objects.fileProperty()

    private val _kernelResources = project.files()

    @Throws(GradleException::class)
    fun validate(): KernelInstallSpec {
        if (!this.getKernelName().matches(Regex("^[a-zA-Z0-9._\\-]+$")))
            throw GradleException("Invalid kernel name '${this.getKernelName()}' must match '^[a-zA-Z0-9._\\-]+\$'")
        if (this.getKernelInterruptMode() !in setOf("message", "signal"))
            throw GradleException("Invalid interrupt mode '${this.getKernelInterruptMode()}' should be either 'message' or 'signal'")
        return this
    }


    @Input
    fun getKernelName(): String = this._kernelName.get()
    fun setKernelName(kernelName: String) = this._kernelName.set(kernelName)
    fun setKernelName(kernelName: Provider<String>) = this._kernelName.set(kernelName)


    @Input
    fun getKernelDisplayName(): String = this._kernelDisplayName.get()
    fun setKernelDisplayName(kernelDisplayName: String) = this._kernelDisplayName.set(kernelDisplayName)
    fun setKernelDisplayName(kernelDisplayName: Provider<String>) = this._kernelDisplayName.set(kernelDisplayName)


    @Input
    fun getKernelLanguage(): String = this._kernelLanguage.get()
    fun setKernelLanguage(kernelLanguage: String) = this._kernelLanguage.set(kernelLanguage)
    fun setKernelLanguage(kernelLanguage: Provider<String>) = this._kernelLanguage.set(kernelLanguage)


    @InputFile
    fun getKernelExecutable(): RegularFile = this._kernelExecutable.get()
    fun setKernelExecutable(kernelExecutable: RegularFile) = this._kernelExecutable.set(kernelExecutable)
    fun setKernelExecutable(kernelExecutable: Provider<RegularFile>) = this._kernelExecutable.set(kernelExecutable)


    @Input
    fun getKernelEnv(): Map<String, String> = this._kernelEnv.get()
    fun setKernelEnv(kernelEnv: Map<String, String>) = this._kernelEnv.set(kernelEnv)
    fun setKernelEnv(kernelEnv: Provider<Map<String, String>>) = this._kernelEnv.set(kernelEnv)


    @Input
    fun getKernelInterruptMode(): String = this._kernelInterruptMode.get()
    fun setKernelInterruptMode(kernelInterruptMode: String) = this._kernelInterruptMode.set(kernelInterruptMode)
    fun setKernelInterruptMode(kernelInterruptMode: Provider<String>) = this._kernelInterruptMode.set(kernelInterruptMode)


    @InputFiles
    fun getKernelResources(): FileCollection = this._kernelResources
    fun setKernelResources(kernelResources: FileCollection) = this._kernelResources.setFrom(kernelResources)
}