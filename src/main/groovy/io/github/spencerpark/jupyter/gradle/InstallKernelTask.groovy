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
import org.gradle.api.DefaultTask
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

@CompileStatic
class InstallKernelTask extends DefaultTask {
    private final KernelInstallProperties _kernelInstallProps
    private final PropertyState<File> _kernelInstallPath

    InstallKernelTask() {
        this._kernelInstallProps = new KernelInstallProperties(super.project)
        this._kernelInstallPath = super.project.property(File.class)
    }


    @Nested
    KernelInstallProperties getKernelInstallProps() {
        return this._kernelInstallProps
    }

    void kernelInstallProps(Action<? extends KernelInstallProperties> configure) {
        configure.execute(this._kernelInstallProps)
    }


    @OutputDirectory
    File getKernelInstallPath() {
        return this._kernelInstallPath.get()
    }

    void setKernelInstallPath(File kernelInstallPath) {
        this._kernelInstallPath.set(kernelInstallPath)
    }

    void setKernelInstallPath(Provider<File> kernelInstallPath) {
        this._kernelInstallPath.set(kernelInstallPath)
    }


    @OutputDirectory
    File getKernelDirectory() {
        return new File([this.getKernelInstallPath().absolutePath, 'kernels', this._kernelInstallProps.getKernelLanguage()]
                .join(File.separator))
    }


    @Internal
    File getInstalledKernelJar() {
        return new File(getKernelDirectory(), this._kernelInstallProps.getKernelExecutable().name)
    }

    // -------------------------------------------------------------
    // Task execution
    // -------------------------------------------------------------

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        super.project.copySpec()
        this.writeKernelSpec()
        super.project.copy {
            from this._kernelInstallProps.getKernelResources()
            from this._kernelInstallProps.getKernelExecutable()
            into this.getKernelDirectory()
        }
    }

    private void writeKernelSpec() {
        KernelSpec spec = new KernelSpec(
                this.getInstalledKernelJar(),
                this._kernelInstallProps.getKernelDisplayName(),
                this._kernelInstallProps.getKernelLanguage(),
                this._kernelInstallProps.getKernelEnv())

        File kernelSpec = new File(this.getKernelDirectory(), 'kernel.json')
        kernelSpec.text = spec.toString()
    }
}
