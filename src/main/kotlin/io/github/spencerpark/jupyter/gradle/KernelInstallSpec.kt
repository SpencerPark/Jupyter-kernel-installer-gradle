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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles

class KernelInstallSpec(objects: ObjectFactory) {
    @Input val kernelName: Property<String> = objects.property(String::class.java)
    @Input val kernelDisplayName: Property<String> = objects.property(String::class.java)
    @Input val kernelLanguage: Property<String> = objects.property(String::class.java)

    @Input val kernelInterruptMode: Property<String> = objects.property(String::class.java)

    @Input val kernelEnv: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java)

    @InputFile val kernelExecutable: RegularFileProperty = objects.fileProperty()

    @InputFiles val kernelResources: ConfigurableFileCollection = objects.fileCollection()

    @Throws(GradleException::class)
    fun validate(): KernelInstallSpec {
        if (!kernelName.get().matches(Regex("^[a-zA-Z0-9._\\-]+$")))
            throw GradleException("Invalid kernel name '${kernelName.get()}' must match '^[a-zA-Z0-9._\\-]+\$'")
        if (kernelInterruptMode.get() !in setOf("message", "signal"))
            throw GradleException("Invalid interrupt mode '${kernelInterruptMode.get()}' should be either 'message' or 'signal'")
        return this
    }

    fun setKernelResources(kernelResources: FileCollection) = this.kernelResources.setFrom(kernelResources)
}