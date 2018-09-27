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

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input

@CompileStatic
class KernelJson {
    private final String _compiledSpec

    private final String _installedKernelJar
    private final String _kernelDisplayName
    private final String _kernelLanguage
    private final String _kernelInterruptMode
    private final Map<String, String> _kernelEnvironment

    KernelJson(File installedKernelJar, String kernelDisplayName, String kernelLanguage, String interruptMode, Map<String, String> kernelEnvironment) {
        this(installedKernelJar.absolutePath.toString().replace(File.separatorChar, '/' as char), kernelDisplayName, kernelLanguage, interruptMode, kernelEnvironment)
    }

    KernelJson(String installedKernelJar, String kernelDisplayName, String kernelLanguage, String interruptMode, Map<String, String> kernelEnvironment) {
        this._installedKernelJar = installedKernelJar
        this._kernelDisplayName = kernelDisplayName
        this._kernelLanguage = kernelLanguage
        this._kernelInterruptMode = interruptMode
        this._kernelEnvironment = kernelEnvironment

        this._compiledSpec = JsonOutput.prettyPrint(
                JsonOutput.toJson(
                        argv: ['java', '-jar', getInstalledKernelJar(), '{connection_file}'],
                        display_name: getKernelDisplayName(),
                        language: getKernelLanguage(),
                        interrupt_mode: getKernelInterruptMode(),
                        env: getKernelEnv()
                )
        )
    }

    @Input
    String getInstalledKernelJar() {
        return _installedKernelJar
    }

    @Input
    String getKernelDisplayName() {
        return _kernelDisplayName
    }

    @Input
    String getKernelLanguage() {
        return _kernelLanguage
    }

    @Input
    String getKernelInterruptMode() {
        return _kernelInterruptMode
    }

    @Input
    Map<String, String> getKernelEnv() {
        return _kernelEnvironment
    }

    @Override
    String toString() {
        return _compiledSpec
    }
}
