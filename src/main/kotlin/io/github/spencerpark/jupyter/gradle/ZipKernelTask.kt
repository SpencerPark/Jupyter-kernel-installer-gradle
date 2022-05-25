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

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import io.github.spencerpark.jupyter.gradle.installers.InstallerMethod
import io.github.spencerpark.jupyter.gradle.installers.InstallerParameterSpec
import io.github.spencerpark.jupyter.gradle.installers.InstallersSpec
import io.github.spencerpark.jupyter.gradle.installers.PythonScriptGenerator
import io.github.spencerpark.jupyter.gradle.installers.SimpleScriptGenerator
import org.gradle.api.Action
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.GeneratedSingletonFileTree
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.util.PatternSet
import org.gradle.internal.nativeintegration.services.FileSystems
import org.gradle.util.ConfigureUtil
import java.io.OutputStream
import javax.inject.Inject

open class ZipKernelTask @Inject constructor(objects: ObjectFactory) : Zip(), WithGradleDslExtensions {
    companion object {
        /**
         * A placeholder identifier that is added into the kernel.json to be replaced when installed. The
         * path to the kernel is not known until it is installed which is why this field needs to be
         * deferred.
         */
        private const val UNSET_PATH_TOKEN = "@KERNEL_INSTALL_DIRECTORY@"

        /**
         * The path relative to the zip archive root directory, to the kernel.json file.
         */
        private const val KERNEL_JSON_PATH = "kernel.json"

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

    /**
     * The properties of the kernel being install. These will end up inside the generated kernel.json.
     */
    private val _kernelInstallSpec = KernelInstallSpec(objects)

    /**
     * A specification of which installers to generate and include in the output zip.
     */
    private val _installers = InstallersSpec()

    /**
     * Parameters during installation that may be configured.
     */
    private val _kernelParameters = KernelParameterSpecContainer(objects)

    init {
        this._installers.with("python")

        // Thanks org/gradle/jvm/tasks/Jar.java for all your help :)

        /**
         * A virtual file that is generated before being included in the output zip file. It is the
         * kernel's configuration file that is used by Jupyter to launch the kernel.
         */
        val kernelJson = mainSpec.addFirst()
        kernelJson.addChild().from({
            return@from generatedFileTree(KERNEL_JSON_PATH, Action { out ->
                val spec = with(kernelInstallSpec) {
                    KernelJson(
                            "$UNSET_PATH_TOKEN/${kernelExecutable.get().asFile.name}",
                            kernelDisplayName.get(),
                            kernelLanguage.get(),
                            kernelInterruptMode.get(),
                            kernelEnv.get()
                    )
                }

                out.write(spec.toString().toByteArray(Charsets.UTF_8))
            })
        })

        mainSpec.appendCachingSafeCopyAction { details ->
            if (details.path.equals(KERNEL_JSON_PATH, ignoreCase = true))
                details.exclude()
        }

        mainSpec.from(kernelInstallSpec.kernelResources)
        mainSpec.from(kernelInstallSpec.kernelExecutable)

        mainSpec.into(kernelInstallSpec.kernelName)

        val installerScriptsSpec = rootSpec.addChild().into("")
        installerScriptsSpec.from({
            if (InstallerMethod.PYTHON_SCRIPT in installers) {
                val generator = PythonScriptGenerator()
                kernelParameters.params.get().forEach { generator.addParameter(compileParam(it)) }
                return@from generatedFileTree("install.py", loadTemplate("install-scripts/python/install.template.py", generator))
            }

            return@from project.files()
        })
    }

    private fun generatedFileTree(fileName: String, generator: Action<OutputStream>): FileTree {
        val tree = GeneratedSingletonFileTree(
            super.getTemporaryDirFactory(),
            fileName,
            {  },
            generator,
            FileSystems.getDefault()
        )
        // TODO Is this the correct PatternSet factory?
        // TODO Default PatternSet() is used in constructor for GeneratedSingletonFileTree for gradle version 6
        return FileTreeAdapter(tree) { PatternSet() }
    }

    private fun loadTemplate(path: String, generator: SimpleScriptGenerator): Action<OutputStream> {
        return Action { out: OutputStream ->
            val resource = ZipKernelTask::class.java.classLoader.getResourceAsStream(path)
                    ?: throw IllegalArgumentException("Template path not found in class resources: $path")
            val source = resource.bufferedReader().readText()

            val name = kernelInstallSpec.kernelName.get()
            generator["KERNEL_NAME"] = name
            generator["KERNEL_DIRECTORY"] = name

            generator.compile(source).write(out)
        }
    }


    @Option(option = "archive-name", description = "Set the name of the output archive.")
    fun setArchiveFileName(name: String) {
        super.getArchiveFileName().set(name)
    }


    val kernelInstallSpec: KernelInstallSpec
        @Nested get() = this._kernelInstallSpec

    fun kernelInstallSpec(configure: Action<in KernelInstallSpec>): ZipKernelTask {
        configure.execute(kernelInstallSpec)
        return this
    }

    fun kernelInstallSpec(@DelegatesTo(value = KernelInstallSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>): ZipKernelTask {
        ConfigureUtil.configure(configureClosure, kernelInstallSpec)
        return this
    }


    val installers: InstallersSpec
        @Nested get() = this._installers

    fun installers(configure: Action<in InstallersSpec>): ZipKernelTask {
        configure.execute(installers)
        return this
    }

    fun installers(@DelegatesTo(value = InstallersSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>): ZipKernelTask {
        ConfigureUtil.configure(configureClosure, installers)
        return this
    }

    fun withInstaller(vararg methods: Any?): InstallersSpec {
        return installers.with(*methods)
    }

    fun withoutInstaller(vararg methods: Any?): InstallersSpec {
        return installers.without(*methods)
    }

    @Option(option = "with", description = "Include an installer in the zipped bundle.")
    fun withInstaller(installerName: List<String>) {
        installers.with(installerName)
    }

    @Option(option = "without", description = "Exclude an installer in the zipped bundle.")
    fun withoutInstaller(installerName: List<String>) {
        installers.without(installerName)
    }


    val kernelParameters: KernelParameterSpecContainer
        @Nested get() = this._kernelParameters

    fun kernelParameters(configure: Action<in KernelParameterSpecContainer>): ZipKernelTask {
        configure.execute(kernelParameters)
        return this
    }

    fun kernelParameters(@DelegatesTo(value = KernelParameterSpecContainer::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>): ZipKernelTask {
        ConfigureUtil.configure(configureClosure, kernelParameters)
        return this
    }
}
