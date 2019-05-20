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
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.ConfigureUtil

@CompileStatic
class KernelExtension {
    private final Project _project

    private final Property<String> _kernelName
    private final Property<String> _kernelDisplayName
    private final Property<String> _kernelLanguage

    private final Property<String> _kernelInterruptMode

    private final MapProperty<String, String> _kernelEnv

    private final RegularFileProperty _kernelExecutable

    private final ConfigurableFileCollection _kernelResources

    private final Property<KernelParameterSpecContainer> _kernelParameters

    KernelExtension(Project project) {
        this._project = project

        this._kernelName = project.objects.property(String).convention(project.name)
        this._kernelDisplayName = project.objects.property(String).convention(this._kernelName)
        this._kernelLanguage = project.objects.property(String).convention(this._kernelName)

        this._kernelInterruptMode = project.objects.property(String).convention('message')

        this._kernelEnv = project.objects.mapProperty(String, String).convention([:])

        this._kernelExecutable = project.objects.fileProperty().convention(
                (project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar).archiveFile)

        this._kernelResources = project.files(project.fileTree('kernel'))

        this._kernelParameters = project.objects.property(KernelParameterSpecContainer).convention(
                new KernelParameterSpecContainer(project))
    }


    String getKernelName() {
        return this._kernelName.get()
    }

    Provider<String> getKernelNameProvider() {
        return this._kernelName
    }

    void setKernelName(String kernelName) {
        this._kernelName.set(kernelName)
    }


    String getKernelDisplayName() {
        return this._kernelDisplayName.get()
    }

    Provider<String> getKernelDisplayNameProvider() {
        return this._kernelDisplayName
    }

    void setKernelDisplayName(String kernelDisplayName) {
        this._kernelDisplayName.set(kernelDisplayName)
    }


    String getKernelLanguage() {
        return this._kernelLanguage.get()
    }

    Provider<String> getKernelLanguageProvider() {
        return this._kernelLanguage
    }

    void setKernelLanguage(String kernelLanguage) {
        this._kernelLanguage.set(kernelLanguage)
    }


    String getKernelInterruptMode() {
        return this._kernelInterruptMode.get()
    }

    Provider<String> getKernelInterruptModeProvider() {
        return this._kernelInterruptMode
    }

    void setKernelInterruptMode(String kernelInterruptMode) {
        this._kernelInterruptMode.set(kernelInterruptMode)
    }


    Map<String, String> getKernelEnv() {
        return this._kernelEnv.get()
    }

    Provider<Map<String, String>> getKernelEnvProvider() {
        return this._kernelEnv
    }

    void setKernelEnv(Map<String, String> kernelEnv) {
        this._kernelEnv.set(kernelEnv)
    }

    void kernelEnv(Map<String, String> kernelEnv) {
        this._kernelEnv.putAll(kernelEnv)
    }

    void kernelEnv(Action<? super Map<String, String>> kernelEnvAction) {
        Map<String, String> copy = [:]
        copy.putAll(this._kernelEnv.get())
        kernelEnvAction.execute(copy)
        this._kernelEnv.set(copy)
    }


    RegularFile getKernelExecutable() {
        return this._kernelExecutable.get()
    }

    Provider<RegularFile> getKernelExecutableProvider() {
        return this._kernelExecutable
    }

    void setKernelExecutable(RegularFile file) {
        this._kernelExecutable.set(file)
    }


    FileCollection getKernelResources() {
        return this._kernelResources
    }

    void setKernelResources(FileCollection kernelResources) {
        this._kernelResources.setFrom(kernelResources)
    }

    void kernelResources(
            @DelegatesTo(value = CopySpec, strategy = Closure.DELEGATE_FIRST) Closure configureClosure) {
        CopySpec spec = this._project.copySpec(configureClosure)
        FileCollection src = (spec as CopySpecInternal).buildRootResolver().allSource
        this._kernelResources.setFrom(src)
    }


    KernelParameterSpecContainer getKernelParameters() {
        return this._kernelParameters.get()
    }

    Provider<KernelParameterSpecContainer> getKernelParametersProvider() {
        return this._kernelParameters
    }

    void setKernelParameters(KernelParameterSpecContainer kernelParameters) {
        this._kernelParameters.set(kernelParameters)
    }

    void kernelParameters(
            @DelegatesTo(value = KernelParameterSpecContainer, strategy = Closure.DELEGATE_FIRST) Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, this.getKernelParameters())
    }

    void kernelParameters(Action<? extends KernelParameterSpecContainer> configure) {
        configure.execute(this.getKernelParameters())
    }
}