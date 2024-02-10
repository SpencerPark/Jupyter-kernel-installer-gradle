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

import io.github.spencerpark.jupyter.gradle.KernelInstallSpec
import io.github.spencerpark.jupyter.gradle.KernelJson
import io.github.spencerpark.jupyter.gradle.KernelParameterSpecContainer
import io.github.spencerpark.jupyter.gradle.PropertyNames
import io.github.spencerpark.jupyter.gradle.WithGradleDslExtensions
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
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
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject

@DisableCachingByDefault
abstract class InstallKernelTask @Inject constructor(private val execOps: ExecOperations) : DefaultTask(),
    WithGradleDslExtensions {
    @get:Nested
    abstract val kernelInstallSpec: KernelInstallSpec

    @get:Nested
    abstract val kernelParameters: KernelParameterSpecContainer

    @get:Input
    abstract val providedParameters: MapProperty<String, List<String>>

    @get:Input
    @get:Optional
    abstract val pythonExecutable: Property<String>

    @Internal
    val userInstallPath: Provider<Directory> = project.provider {
        project.layout.projectDirectory.dir(runCommand(getPythonAndCheckValid(), "-m", "jupyter", "--data-dir"))
    }

    @Internal
    val sysPrefixInstallPath: Provider<Directory> = project.provider {
        this.prefixInstallPath(runCommand(getPythonAndCheckValid(), "-c", "import sys; print(sys.prefix)")).get()
    }

    @Internal
    val legacyInstallPath: Provider<Directory> = project.provider {
        project.layout.projectDirectory.dir(
            listOf(
                project.file(System.getProperty("user.home")).absolutePath,
                ".ipython"
            ).joinToString(File.separator)
        )
    }

    @Internal
    val defaultInstallPath: Provider<Directory> = project.provider {
        project.layout.projectDirectory.dir(
            runCommand(
                getPythonAndCheckValid(),
                "-c",
                "import jupyter_core.paths as j; print(j.SYSTEM_JUPYTER_PATH[0])"
            )
        )
    }

    @get:Internal
    abstract val kernelInstallPath: DirectoryProperty

    @get:OutputDirectory
    abstract val kernelDirectory: DirectoryProperty

    init {
        kernelInstallPath.convention(this.commandLineSpecifiedPathOr(this.defaultInstallPath))
        kernelDirectory.convention(project.provider {
            kernelInstallPath.get().dir("kernels").dir(kernelInstallSpec.kernelName.get())
        })
    }

    fun kernelInstallSpec(configure: Action<in KernelInstallSpec>) = configure.execute(this.kernelInstallSpec)

    fun kernelParameters(configure: Action<in KernelParameterSpecContainer>) = configure.execute(this.kernelParameters)

    fun providedParameters(providedParameters: Map<String, Any>) {
        val oldParams = this.providedParameters.get()
        val newParams = mutableMapOf<String, MutableList<String>>()
        oldParams.forEach { (p, vs) -> newParams[p] = vs.toMutableList() }

        providedParameters.forEach { (p, addVs) ->
            newParams.compute(p) { _, _vs ->
                val vs = _vs ?: mutableListOf()

                @Suppress("UNCHECKED_CAST")
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
                throw IllegalArgumentException("Parameter must be of the form 'NAME:VALUE' but was $serializedParam")

            val (name, value) = parts

            val vs = newParams[name] ?: mutableListOf()
            vs.add(value)
            newParams[name] = vs
        }

        this.providedParameters.set(newParams)
    }

    @Option(option = "python", description = "Set the python executable to use for resolving the `sys.prefix`.")
    fun setPythonExecutableOption(pythonExecutable: String) = this.pythonExecutable.set(pythonExecutable)


    @Option(
        option = "path",
        description = "Set the path to install the kernel to. The install directory is \$path/\$kernelName."
    )
    fun setKernelInstallPathOption(kernelInstallPath: String) =
        this.kernelInstallPath.set(project.layout.projectDirectory.dir(kernelInstallPath))

    private fun runCommand(vararg commandLine: String): String {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        val result = execOps.exec { spec ->
            spec.setIgnoreExitValue(true)
            spec.commandLine(*commandLine)
            spec.standardOutput = stdout
            spec.errorOutput = stderr
        }

        val output = stdout.toString(Charset.defaultCharset()).trim() +
                stderr.toString(Charset.defaultCharset()).trim();

        if (result.exitValue != 0) {
            logger.error("Command output: {}", output)
            result.assertNormalExitValue()
        }

        return output
    }

    private fun getPythonAndCheckValid(): String {
        // Use gradle property jupyter.python first, then a system prop, finally fallback to just python3

        val python = project.findProperty(PropertyNames.INSTALL_KERNEL_PYTHON) as String?
            ?: this.pythonExecutable.orNull
            ?: "python3"

        if (!runCommand(python, "--version").startsWith("Python"))
            throw GradleException("Configured python command doesn't look like python: '$python'")

        return python
    }

    @Option(option = "user", description = "Install to the per-user kernel registry.")
    fun setUseUserInstallPath(use: Boolean) {
        if (use)
            kernelInstallPath.set(this.userInstallPath)
    }

    @Option(
        option = "sys-prefix",
        description = "Install to Python's `sys.prefix`. Useful in conda/virtual environments."
    )
    fun setUseSysPrefixInstallPath(use: Boolean) {
        if (use)
            kernelInstallPath.set(this.sysPrefixInstallPath)
    }

    @Option(
        option = "prefix",
        description = "Specify a prefix to install to, e.g. an env. The kernelspec will be installed in `PREFIX/share/jupyter/kernels/`."
    )
    fun setUsePrefixInstallPath(prefix: String) = kernelInstallPath.set(this.prefixInstallPath(prefix))

    fun prefixInstallPath(prefix: String): Provider<Directory> = project.provider {
        project.layout.projectDirectory.dir(
            listOf(
                project.file(prefix).absolutePath,
                "share",
                "jupyter"
            ).joinToString(File.separator)
        )
    }

    @Option(option = "legacy", description = "Install to `\$HOME/.ipython`. Not recommended but available if needed.")
    fun setUseLegacyInstallPath(use: Boolean) {
        if (use)
            kernelInstallPath.set(this.legacyInstallPath)
    }

    @Option(option = "default", description = "Install for all users.")
    fun setUseDefaultInstallPath(use: Boolean) {
        if (use)
            kernelInstallPath.set(this.defaultInstallPath)
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
    fun commandLineSpecifiedPathOr(fallback: Provider<Directory> = this.defaultInstallPath): Provider<Directory> =
        project.provider {
            val pathProp = project.findProperty(PropertyNames.INSTALL_KERNEL_PATH) as String?
                ?: return@provider fallback.get()

            when (pathProp) {
                "@USER@" ->
                    return@provider this.userInstallPath.get()

                "@SYS_PREFIX@" ->
                    return@provider this.sysPrefixInstallPath.get()

                "@LEGACY@" ->
                    return@provider this.legacyInstallPath.get()

                else ->
                    return@provider if (pathProp.trim().isEmpty())
                        this.defaultInstallPath.get()
                    else
                        this.prefixInstallPath(pathProp).get()
            }
        }

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
            KernelJson(
                getInstalledKernelJar(),
                kernelDisplayName.get(),
                kernelLanguage.get(),
                kernelInterruptMode.get(),
                env,
                kernelMetadata.get(),
            )
        }
    }


    private fun getInstalledKernelJar(): RegularFile =
        kernelDirectory.get().file(this.kernelInstallSpec.kernelExecutable.get().asFile.name)

    @TaskAction
    fun execute() {
        val kernelSpecFile = kernelDirectory.file("kernel.json").get()

        kernelSpecFile.asFile.writeText(this.getKernelSpec().toString())

        project.sync { spec ->
            spec.into(kernelDirectory)
            spec.preserve {
                it.include("kernel.json")
            }

            spec.from(kernelInstallSpec.kernelResources)
            spec.from(kernelInstallSpec.kernelExecutable)
        }
    }
}
