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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles

@CompileStatic
class KernelInstallSpec {
    private final PropertyState<String> _kernelName
    private final PropertyState<String> _kernelDisplayName
    private final PropertyState<String> _kernelLanguage

    private final PropertyState<String> _kernelInterruptMode

    private final PropertyState<Map<String, String>> _kernelEnv

    private final PropertyState<File> _kernelExecutable

    private final ConfigurableFileCollection _kernelResources

    KernelInstallSpec(Project project) {
        this._kernelName = project.property(String.class)
        this._kernelDisplayName = project.property(String.class)
        this._kernelLanguage = project.property(String.class)

        this._kernelInterruptMode = project.property(String.class)

        this._kernelEnv = (project.property(Map.class) as PropertyState<Map<String, String>>)

        this._kernelExecutable = project.property(File.class)

        this._kernelResources = project.files()
    }

    KernelInstallSpec validate() throws GradleException {
        if (!(this.getKernelName() ==~ /^[a-zA-Z0-9._\-]+$/))
            throw new GradleException("Invalid kernel name '${this.getKernelName()}' must match '^[a-zA-Z0-9._\\-]+\$'")
        if (!(this.getKernelInterruptMode() in ['message', 'signal']))
            throw new GradleException("Invalid interrupt mode '${this.getKernelInterruptMode()}' should be either 'message' or 'signal'")
        return this
    }


    @Input
    String getKernelName() {
        return this._kernelName.get()
    }

    void setKernelName(String kernelName) {
        this._kernelName.set(kernelName)
    }

    void setKernelName(Provider<String> kernelName) {
        this._kernelName.set(kernelName)
    }


    @Input
    String getKernelDisplayName() {
        return this._kernelDisplayName.get()
    }

    void setKernelDisplayName(String kernelDisplayName) {
        this._kernelDisplayName.set(kernelDisplayName)
    }

    void setKernelDisplayName(Provider<String> kernelDisplayName) {
        this._kernelDisplayName.set(kernelDisplayName)
    }


    @Input
    String getKernelLanguage() {
        return this._kernelLanguage.get()
    }

    void setKernelLanguage(String kernelLanguage) {
        this._kernelLanguage.set(kernelLanguage)
    }

    void setKernelLanguage(Provider<String> kernelLanguage) {
        this._kernelLanguage.set(kernelLanguage)
    }


    @InputFile
    File getKernelExecutable() {
        return this._kernelExecutable.get()
    }

    void setKernelExecutable(File kernelExecutable) {
        this._kernelExecutable.set(kernelExecutable)
    }

    void setKernelExecutable(Provider<File> kernelExecutable) {
        this._kernelExecutable.set(kernelExecutable)
    }


    @Input
    Map<String, String> getKernelEnv() {
        return this._kernelEnv.get()
    }

    void setKernelEnv(Map<String, String> kernelEnv) {
        this._kernelEnv.set(kernelEnv)
    }

    void setKernelEnv(Provider<Map<String, String>> kernelEnv) {
        this._kernelEnv.set(kernelEnv)
    }


    @Input
    String getKernelInterruptMode() {
        return this._kernelInterruptMode.get()
    }

    void setKernelInterruptMode(String kernelInterruptMode) {
        this._kernelInterruptMode.set(kernelInterruptMode)
    }

    void setKernelInterruptMode(Provider<String> kernelInterruptMode) {
        this._kernelInterruptMode.set(kernelInterruptMode)
    }


    @InputFiles
    FileCollection getKernelResources() {
        return this._kernelResources
    }

    void setKernelResources(FileCollection kernelResources) {
        this._kernelResources.setFrom(kernelResources)
    }
}