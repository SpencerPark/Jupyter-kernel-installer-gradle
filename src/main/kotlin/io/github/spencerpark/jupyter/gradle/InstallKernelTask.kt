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
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.util.ConfigureUtil
import java.io.File
import java.util.concurrent.Callable
import javax.inject.Inject

// TODO use ExecOperations injection to run commands
@Suppress("UNCHECKED_CAST")
open class InstallKernelTask @Inject constructor(objects: ObjectFactory) : DefaultTask(), WithGradleDslExtensions {
    @Nested val kernelInstallSpec = KernelInstallSpec(objects)
    @Nested val kernelParameters = KernelParameterSpecContainer(objects)
    @Input val providedParameters: MapProperty<String, List<String>> = objects.mapProperty(String::class.java, List::class.java).convention(mutableMapOf()) as MapProperty<String, List<String>>
    // @Optional already applied to getPythonExecutable
    private val pythonExecutable: Property<String> = objects.property(String::class.java)
    @OutputDirectory @Optional val kernelInstallPath: DirectoryProperty = objects.directoryProperty().convention(project.provider {
        this.commandLineSpecifiedPath(this.defaultInstallPath).call()
    })

    fun kernelInstallSpec(configure: Action<in KernelInstallSpec>) = configure.execute(this.kernelInstallSpec)
    fun kernelInstallSpec(@DelegatesTo(value = KernelInstallSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) {
        ConfigureUtil.configure(configureClosure, this.kernelInstallSpec)
    }

    fun kernelParameters(configure: Action<in KernelParameterSpecContainer>) = configure.execute(this.kernelParameters)
    fun kernelParameters(@DelegatesTo(value = KernelParameterSpecContainer::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) {
        ConfigureUtil.configure(configureClosure, this.kernelParameters)
    }

    fun providedParameters(providedParameters: Map<String, Any>) {
        val oldParams = this.providedParameters.get()
        val newParams = mutableMapOf<String, MutableList<String>>()
        oldParams.forEach { (p, vs) -> newParams[p] = vs.toMutableList() }

        providedParameters.forEach { (p, addVs) ->
            newParams.compute(p) { _, _vs ->
                val vs = _vs ?: mutableListOf()

                if (addVs is Iterable<*>)
                    vs.addAll(addVs as Iterable<String>)
                else
                    vs.add(addVs as String)

                return@compute vs
            }
        }

        this.providedParameters.set(newParams)
    }

    @Option(option = "param", description = "Add a provided parameter with the form \"NAME:VALUE\"")
    fun addProvidedParams(serializedParams: List<String>) {
        val oldParams = this.providedParameters.get()
        val newParams = mutableMapOf<String, MutableList<String>>()
        oldParams.forEach { (p, vs) -> newParams[p] = vs.toMutableList() }

        serializedParams.forEach { serializedParam ->
            val parts = serializedParam.split(":", limit = 2)

            if (parts.size != 2)
                throw IllegalArgumentException("Parameter must be of the form 'NAME=VALUE' but was $serializedParam")

            val (name, value) = parts

            val vs = newParams[name] ?: mutableListOf()
            vs.add(value)
            newParams[name] = vs
        }

        this.providedParameters.set(newParams)
    }


    @Optional
    @Input
    fun getPythonExecutable(): String? = project.findProperty(PropertyNames.INSTALL_KERNEL_PYTHON) as String?
            ?: this.pythonExecutable.orNull

    @Option(option = "python", description = "Set the python executable to use for resolving the `sys.prefix`.")
    fun setPythonExecutable(pythonExecutable: String) = this.pythonExecutable.set(pythonExecutable)


    @Option(option = "path", description = "Set the path to install the kernel to. The install directory is \$path/\$kernelName.")
    fun setKernelInstallPath(kernelInstallPath: String) = this.kernelInstallPath.set(project.layout.projectDirectory.dir(kernelInstallPath))
    fun setKernelInstallPath(kernelInstallPath: Callable<Directory>) = this.kernelInstallPath.set(project.provider(kernelInstallPath))

    private fun runCommand(command: String): String {
        val process = Runtime.getRuntime().exec(command)
        if (process.waitFor() != 0) {
            val stdout = process.inputStream.bufferedReader().lineSequence().joinToString("\t\n")
            val stderr = process.errorStream.bufferedReader().lineSequence().joinToString("\t\n")
            project.logger.error("Failed to execute: $command.\nStdout:\n\t$stdout\nStderr:\n\t$stderr")

            throw GradleException("Could not get jupyter data-dir.")
        }

        // Java named these a bit weird, "in" is stdout of the process
        return process.inputStream.bufferedReader().readText().trim() + process.errorStream.bufferedReader().readText().trim()
    }

    private fun getPythonAndCheckValid(): String {
        // Use gradle property jupyter.python first, then a system prop, finally fallback to just python3
        val python = this.getPythonExecutable() ?: "python3"

        if (!runCommand("$python --version").startsWith("Python"))
            throw GradleException("Configured python command doesn't look like python: '$python'")

        return python
    }

    @Option(option = "user", description = "Install to the per-user kernel registry.")
    fun setUseUserInstallPath(use: Boolean) {
        if (use)
            this.setKernelInstallPath(this.userInstallPath)
    }

    @Internal
    val userInstallPath = Callable {
        project.layout.projectDirectory.dir(runCommand("${getPythonAndCheckValid()} -m jupyter --data-dir"))
    }

    @Option(option = "sys-prefix", description = "Install to Python's `sys.prefix`. Useful in conda/virtual environments.")
    fun setUseSysPrefixInstallPath(use: Boolean) {
        if (use)
            this.setKernelInstallPath(this.sysPrefixInstallPath)
    }

    @Internal
    val sysPrefixInstallPath = Callable {
        this.prefixInstallPath(runCommand("${getPythonAndCheckValid()} - c \"import sys;print(sys.prefix)\"")).call()
    }

    @Option(option = "prefix", description = "Specify a prefix to install to, e.g. an env. The kernelspec will be installed in `PREFIX/share/jupyter/kernels/`.")
    fun setUsePrefixInstallPath(prefix: String) = this.setKernelInstallPath(this.prefixInstallPath(prefix))

    fun prefixInstallPath(prefix: String) = Callable {
        project.layout.projectDirectory.dir(listOf(
                project.file(prefix).absolutePath,
                "share",
                "jupyter"
        ).joinToString(File.separator))
    }

    @Option(option = "legacy", description = "Install to `\$HOME/.ipython`. Not recommended but available if needed.")
    fun setUseLegacyInstallPath(use: Boolean) {
        if (use)
            this.setKernelInstallPath(this.legacyInstallPath)
    }

    @Internal
    val legacyInstallPath = Callable {
        project.layout.projectDirectory.dir(listOf(
                project.file(System.getProperty("user.home")).absolutePath,
                ".ipython"
        ).joinToString(File.separator))
    }

    @Option(option = "default", description = "Install for all users.")
    fun setUseDefaultInstallPath(use: Boolean) {
        if (use)
            this.setKernelInstallPath(this.defaultInstallPath)
    }

    @Internal
    val defaultInstallPath = Callable {
        project.layout.projectDirectory.dir(runCommand("${getPythonAndCheckValid()} - c \"import jupyter_core.paths as j; print(j.SYSTEM_JUPYTER_PATH[0])\""))
    }

    /**
     * Attempts to construct a path from the command line as passed in
     * via a property (-P flag). Special values include:
     * <ul>
     * <li>{@code "@USER@"} - {@link #userInstallPath}</li>
     * <li>{@code "@SYS_PREFIX@"} - {@link #sysPrefixInstallPath}</li>
     * <li>{@code "@LEGACY@"} - {@link #legacyInstallPath}</li>
     * <li>{@code ""} - {@link #defaultInstallPath}</li>
     * <li><i>any prefix path</i> - {@link #prefixInstallPath(String)}</li>
     * </ul>
     *
     * @return the path as resolved according to the rules described above or
     * {@code null} if the property is unset.
     */
    fun commandLineSpecifiedPath(fallback: Callable<Directory> = this.defaultInstallPath) = Callable {
        val pathProp = project.findProperty(PropertyNames.INSTALL_KERNEL_PATH) as String?
                ?: return@Callable fallback.call()

        when (pathProp) {
            "@USER@" ->
                return@Callable this.userInstallPath.call()
            "@SYS_PREFIX@" ->
                return@Callable this.sysPrefixInstallPath.call()
            "@LEGACY@" ->
                return@Callable this.legacyInstallPath.call()
            else ->
                return@Callable if (pathProp.trim().isEmpty())
                    this.defaultInstallPath.call()
                else
                    this.prefixInstallPath(pathProp).call()
        }
    }


    @OutputDirectory
    fun getKernelDirectory(): Directory = this.kernelInstallPath.get().dir("kernels").dir(this.kernelInstallSpec.kernelName.get())


    @Nested
    fun getKernelSpec(): KernelJson {
        val env = this.kernelInstallSpec.kernelEnv.get().toMutableMap() // copy the default
        val providedParams: Map<String, List<String>> = this.providedParameters.get()

        this.kernelParameters.params.get().forEach { param ->
            var handledSomeParam = false
            fun handleParamValue(value: String) {
                handledSomeParam = true
                param.addValueToEnv(param.preProcessAndValidateValue(value), env)
            }

            providedParams[param.name]?.run {
                return@forEach forEach(::handleParamValue)
            }

            var value = project.findProperty(PropertyNames.INSTALL_KERNEL_PROP_PREFIX + param.name) as String?
            value?.let(::handleParamValue)

            // Check for usage with an indexed prop

            // Try index 0
            value = project.findProperty("${PropertyNames.INSTALL_KERNEL_PROP_PREFIX}${param.name}.0") as String?
            value?.let(::handleParamValue)

            // Try index 1 and continue counting up by 1 to get as many props as passed
            var i = 1
            value = project.findProperty("${PropertyNames.INSTALL_KERNEL_PROP_PREFIX}${param.name}.$i") as String?
            while (value != null) {
                handleParamValue(value)
                i++
                value = project.findProperty("${PropertyNames.INSTALL_KERNEL_PROP_PREFIX}${param.name}.$i") as String?
            }

            // If none of the above attempts to find a property worked and the param has a default value,
            // just use the default.
            if (!handledSomeParam)
                param.defaultValue.orNull?.let(::handleParamValue)
        }

        return with(this.kernelInstallSpec) {
            KernelJson(getInstalledKernelJar(), kernelDisplayName.get(), kernelLanguage.get(), kernelInterruptMode.get(), env)
        }
    }


    private fun getInstalledKernelJar(): RegularFile = this.getKernelDirectory().file(this.kernelInstallSpec.kernelExecutable.get().asFile.name)

    @TaskAction
    fun execute() {
        val kernelSpecFile = this.getKernelDirectory().file("kernel.json")
        kernelSpecFile.asFile.writeText(this.getKernelSpec().toString())

        // TODO inject the copy service
        project.copy { spec ->
            spec.from(this.kernelInstallSpec.kernelResources)
            spec.from(this.kernelInstallSpec.kernelExecutable)
            spec.into(this.getKernelDirectory())
        }
    }
}
