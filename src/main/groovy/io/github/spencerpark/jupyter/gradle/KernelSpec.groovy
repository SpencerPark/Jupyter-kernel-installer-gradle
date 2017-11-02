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

class KernelSpec {
    private static final String KERNELSPEC_TEMPLATE_PATH = '/kernel.json.template'

    private final String compiledSpec

    private final File installedKernelJar
    private final String kernelDisplayName
    private final String kernelLanguage
    private final Map<String, String> kernelEnvironment

    KernelSpec(File installedKernelJar, String kernelDisplayName, String kernelLanguage, Map<String, String> kernelEnvironment) {
        this.installedKernelJar = installedKernelJar
        this.kernelDisplayName = kernelDisplayName
        this.kernelLanguage = kernelLanguage
        this.kernelEnvironment = kernelEnvironment

        this.compiledSpec = JsonOutput.prettyPrint(
                JsonOutput.toJson(
                        argv: ['java', '-jar', getInstalledKernelJar().absolutePath.toString().replace(File.separatorChar, '/' as char), '{connection_file}'],
                        display_name: getKernelDisplayName(),
                        language: getKernelLanguage(),
                        env: getKernelEnv()
                )
        )
    }

    File getInstalledKernelJar() {
        return installedKernelJar
    }

    String getKernelDisplayName() {
        return kernelDisplayName
    }

    String getKernelLanguage() {
        return kernelLanguage
    }

    Map<String, String> getKernelEnv() {
        return kernelEnvironment
    }

    @Override
    String toString() {
        return compiledSpec
    }
}