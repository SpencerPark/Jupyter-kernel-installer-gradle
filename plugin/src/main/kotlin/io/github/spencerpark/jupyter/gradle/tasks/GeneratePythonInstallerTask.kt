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

package io.github.spencerpark.jupyter.gradle.tasks

import io.github.spencerpark.jupyter.gradle.KernelParameterSpec
import io.github.spencerpark.jupyter.gradle.KernelParameterSpecContainer
import io.github.spencerpark.jupyter.gradle.ListSpec
import io.github.spencerpark.jupyter.gradle.NumberSpec
import io.github.spencerpark.jupyter.gradle.OneOfSpec
import io.github.spencerpark.jupyter.gradle.StringSpec
import io.github.spencerpark.jupyter.gradle.WithGradleDslExtensions
import io.github.spencerpark.jupyter.gradle.installers.InstallerParameterSpec
import io.github.spencerpark.jupyter.gradle.installers.PythonScriptGenerator
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GeneratePythonInstallerTask : DefaultTask(), WithGradleDslExtensions {
    @get:Input
    abstract val kernelName: Property<String>

    @get:Nested
    abstract val kernelParameters: KernelParameterSpecContainer

    @get:OutputFile
    abstract val output: RegularFileProperty

    init {
        output.convention(project.layout.buildDirectory.file("jupyter/install.py"))
    }

    fun kernelParameters(configure: Action<in KernelParameterSpecContainer>): GeneratePythonInstallerTask {
        configure.execute(kernelParameters)
        return this
    }

    @TaskAction
    fun execute() {
        val generator = PythonScriptGenerator()
        kernelParameters.params.get().forEach { paramSpec ->
            generator.addParameter(compileParam(paramSpec))
        }

        val templatePath = "install-scripts/python/install.template.py"
        val resource = this::class.java.classLoader.getResource(templatePath)
                ?: throw IllegalArgumentException("Template path not found in class resources: $templatePath")
        val source = resource.readText(Charsets.UTF_8)

        val name = kernelName.get()
        generator["KERNEL_NAME"] = name
        generator["KERNEL_DIRECTORY"] = name

        val into = output.get().asFile.outputStream()
        generator.compile(source).write(into)
    }

    private fun compileParam(kSpec: KernelParameterSpec): InstallerParameterSpec {
        val iSpec = InstallerParameterSpec(kSpec.name, kSpec.environmentVariable)
        iSpec.description = kSpec.description.orNull
        iSpec.defaultValue = kSpec.defaultValue.orNull
        iSpec.aliases = kSpec.aliases.get().toMutableMap()

        when (kSpec) {
            is StringSpec -> iSpec.type = InstallerParameterSpec.Type.STRING
            is NumberSpec -> iSpec.type = InstallerParameterSpec.Type.FLOAT
            is ListSpec -> iSpec.listSep = kSpec.separator.get()
            is OneOfSpec -> iSpec.choices = kSpec.values.get().toMutableList()
        }

        return iSpec
    }
}