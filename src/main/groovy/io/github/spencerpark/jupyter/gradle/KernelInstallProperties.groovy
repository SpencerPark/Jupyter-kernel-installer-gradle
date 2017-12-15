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
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles

@CompileStatic
class KernelInstallProperties {
    private final PropertyState<String> _kernelDisplayName
    private final PropertyState<String> _kernelLanguage
    private final PropertyState<Map<String, String>> _kernelEnv
    private final PropertyState<File> _kernelExecutable
    private final ConfigurableFileCollection _kernelResources

    KernelInstallProperties(Project project) {
        this._kernelDisplayName = project.property(String.class)
        this._kernelLanguage = project.property(String.class)
        this._kernelEnv = (project.property(Map.class) as PropertyState<Map<String, String>>)

        this._kernelExecutable = project.property(File.class)
        this._kernelResources = project.files()
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


    @InputFiles
    FileCollection getKernelResources() {
        return this._kernelResources
    }

    void setKernelResources(FileCollection kernelResources) {
        this._kernelResources.setFrom(kernelResources)
    }
}