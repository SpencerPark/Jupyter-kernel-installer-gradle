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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar

class KernelExtension {
    private final PropertyState<String> _kernelDisplayName
    private final PropertyState<String> _kernelLanguage
    private final PropertyState<Map<String, String>> _kernelEnv

    private final PropertyState<File> _kernelExecutable
    private final ConfigurableFileCollection _kernelResources

    private final PropertyState<File> _kernelInstallPath

    KernelExtension(Project project) {
        _kernelDisplayName = project.property(String.class)
        _kernelDisplayName.set(project.name)

        _kernelLanguage = project.property(String.class)
        _kernelLanguage.set(project.name)

        _kernelEnv = project.property(Map.class)
        _kernelEnv.set([:])

        _kernelExecutable = project.property(File.class)
        _kernelExecutable.set(project.provider {
            Jar jarTask = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME) as Jar
            return jarTask.archivePath
        })

        _kernelResources = project.files(project.fileTree('kernel'))

        _kernelInstallPath = project.property(File.class)
        _kernelInstallPath.set(project.provider {
            String USER_HOME = System.getProperty('user.home')
            return new File("$USER_HOME/.ipython")
        })
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
        this.getKernelEnv().putAll(kernelEnv)
    }

    void kernelEnv(Action<? super Map<String, String>> kernelEnvAction) {
        kernelEnvAction.execute(this.getKernelEnv())
    }


    File getKernelExecutable() {
        return this._kernelExecutable.get()
    }

    Provider<File> getKernelExecutableProvider() {
        return this._kernelExecutable
    }

    void setKernelExecutable(File file) {
        this._kernelExecutable.set(file)
    }


    FileCollection getKernelResources() {
        return this._kernelResources
    }

    void setKernelResources(FileCollection kernelResources) {
        this._kernelResources.setFrom(kernelResources)
    }


    File getKernelInstallPath() {
        return this._kernelInstallPath.get()
    }

    Provider<File> getKernelInstallPathProvider() {
        return this._kernelInstallPath
    }

    void setKernelInstallPath(File file) {
        this._kernelInstallPath.set(file)
    }
}