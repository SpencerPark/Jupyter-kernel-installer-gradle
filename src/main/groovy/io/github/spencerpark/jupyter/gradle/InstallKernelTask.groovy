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

import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class InstallKernelTask extends DefaultTask {
    private static final String KERNELSPEC_TEMPLATE_PATH = '/kernel.json.template'

    // -------------------------------------------------------------
    // Task inputs
    // -------------------------------------------------------------

    private final PropertyState<String> _kernelDisplayName = project.property(String.class)
    private final PropertyState<String> _kernelLanguage = project.property(String.class)

    private final PropertyState<File> _kernelExecutable = project.property(File.class)
    private final ConfigurableFileCollection _kernelResources =  project.files()


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


    @InputFiles
    FileCollection getKernelResources() {
        return this._kernelResources
    }

    void setKernelResources(FileCollection kernelResources) {
        this._kernelResources.setFrom(kernelResources)
    }

    // -------------------------------------------------------------
    // Task outputs
    // -------------------------------------------------------------

    private final PropertyState<File> _kernelInstallPath = project.property(File.class)


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
        return new File([getKernelInstallPath().absolutePath, 'kernels', getKernelLanguage()]
                .join(File.separator))
    }


    @Internal
    File getInstalledKernelJar() {
        return new File(getKernelDirectory(), getKernelExecutable().name)
    }

    // -------------------------------------------------------------
    // Task execution
    // -------------------------------------------------------------

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        project.delete(getKernelDirectory().listFiles())
        writeKernelSpec()
        project.copy {
            from getKernelResources()
            from getKernelExecutable()
            into getKernelDirectory()
        }
    }

    private void writeKernelSpec() {
        String compiledSpec

        TemplateEngine templateEngine = new SimpleTemplateEngine()
        InstallKernelTask.class.getResourceAsStream(KERNELSPEC_TEMPLATE_PATH).withReader('UTF-8') {
            template ->
                compiledSpec = templateEngine.createTemplate(template).make(
                        KERNEL_JAR_PATH: getInstalledKernelJar().absolutePath,
                        KERNEL_DISPLAY_NAME: getKernelDisplayName(),
                        KERNEL_LANGUAGE: getKernelLanguage()
                )
        }
        File kernelSpec = new File(getKernelDirectory(), 'kernel.json')
        kernelSpec.text = compiledSpec
    }
}
