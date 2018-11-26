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
import org.gradle.api.GradleException
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.util.ConfigureUtil

import java.util.concurrent.Callable

@CompileStatic
class InstallKernelTask extends DefaultTask {
    private final KernelInstallSpec _kernelInstallSpec
    private final KernelParameterSpecContainer _kernelParameters
    private final PropertyState<Map<String, List<String>>> _providedParameters
    private final PropertyState<String> _pythonExecutable
    private final PropertyState<File> _kernelInstallPath

    InstallKernelTask() {
        this._kernelInstallSpec = new KernelInstallSpec(super.project)

        this._kernelParameters = new KernelParameterSpecContainer(super.project)

        this._providedParameters = (super.project.property(Map.class) as PropertyState<Map<String, List<String>>>)
        this._providedParameters.set(Collections.emptyMap())

        this._pythonExecutable = super.project.property(String.class)

        this._kernelInstallPath = super.project.property(File.class)
        this._kernelInstallPath.set(project.provider(this.commandLineSpecifiedPath(this.defaultInstallPath)))
    }


    @Nested
    KernelInstallSpec getKernelInstallSpec() {
        return this._kernelInstallSpec
    }

    InstallKernelTask kernelInstallSpec(
            @DelegatesTo(value = KernelInstallSpec.class, strategy = Closure.DELEGATE_FIRST) Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, this._kernelInstallSpec)
        return this
    }

    InstallKernelTask kernelInstallSpec(Action<? super KernelInstallSpec> configure) {
        configure.execute(this._kernelInstallSpec)
        return this
    }


    @Nested
    KernelParameterSpecContainer getKernelParameters() {
        return this._kernelParameters
    }

    void kernelParameters(
            @DelegatesTo(value = KernelParameterSpecContainer, strategy = Closure.DELEGATE_FIRST) Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, this.getKernelParameters())
    }

    void kernelParameters(Action<? extends KernelParameterSpecContainer> configure) {
        configure.execute(this.getKernelParameters())
    }


    @Input
    Map<String, List<String>> getProvidedParameters() {
        return this._providedParameters.get()
    }

    void setProvidedParameters(Map<String, List<String>> providedParameters) {
        this._providedParameters.set(providedParameters)
    }

    void setProvidedParameters(Provider<Map<String, List<String>>> providedParametersProvider) {
        this._providedParameters.set(providedParametersProvider)
    }

    void providedParameters(Map<String, Object> providedParameters) {
        Map<String, List<String>> oldParams = this._providedParameters.get()
        Map<String, List<String>> newParams = [:]
        oldParams.forEach{ String p, List<String> vs -> newParams.put(p, new LinkedList<>(vs)) }

        providedParameters.forEach { String p, Object addVs ->
            newParams.compute(p) { _, List<String> vs ->
                if (vs == null)
                    vs = new LinkedList<>()
                if (addVs instanceof Iterable)
                    vs.addAll(addVs)
                else
                    vs.add(addVs as String)
            }
        }

        this._providedParameters.set(newParams)
    }

    @Option(option = 'params', description = 'Add a provided parameter with the form "NAME=VALUE"')
    void addProvidedParams(List<String> serializedParams) {
        Map<String, List<String>> oldParams = this._providedParameters.get()
        Map<String, List<String>> newParams = [:]
        oldParams.forEach { String p, List<String> vs -> newParams.put(p, new LinkedList<>(vs)) }

        serializedParams.forEach { String serializedParam ->
            String[] parts = serializedParam.split(':', 2)

            if (parts.length != 2)
                throw new IllegalArgumentException("Parameter must be of the form 'NAME=VALUE' but was $serializedParam")

            String name = parts[0]
            String value = parts[1]
            newParams.compute(name) { _, List<String> vs ->
                if (vs == null)
                    vs = new LinkedList<>()
                vs.add(value)
            }
        }
    }


    @Optional
    @Input
    String getPythonExecutable() {
        return project.findProperty(PropertyNames.INSTALL_KERNEL_PYTHON) ?: this._pythonExecutable.getOrNull()
    }

    @Option(option = 'python', description = 'Set the python executable to use for installing ')
    void setPythonExecutable(String pythonExecutable) {
        this._pythonExecutable.set(pythonExecutable)
    }

    void setPythonExecutable(Provider<String> pythonExecutable) {
        this._pythonExecutable.set(pythonExecutable)
    }


    @Input
    File getKernelInstallPath() {
        return this._kernelInstallPath.get()
    }

    void setKernelInstallPath(File kernelInstallPath) {
        this._kernelInstallPath.set(kernelInstallPath)
    }

    void setKernelInstallPath(Provider<File> kernelInstallPath) {
        this._kernelInstallPath.set(kernelInstallPath)
    }

    void setKernelInstallPath(Callable<File> kernelInstallPath) {
        this._kernelInstallPath.set(project.provider(kernelInstallPath))
    }

    private String runCommand(String command) {
        Process process = command.execute()
        if (process.waitFor() != 0) {
            String stdout = process.in.text.split('\n').join('\t\n')
            String stderr = process.err.text.split('\n').join('\t\n')
            project.logger.error("Failed to execute: $command.\nStdout:\n\t$stdout\nStderr:\n\t$stderr")

            throw new GradleException("Could not get jupyter data-dir.")
        }

        // Java named these a bit weird, "in" is stdout of the process
        return process.in.text.trim() + process.err.text.trim()
    }

    private String getPythonAndCheckValid() {
        // Use gradle property jupyter.python first, then a system prop, finally fallback to just python3
        String python = pythonExecutable ?: 'python3'

        if (!runCommand("$python --version").startsWith('Python'))
            throw new GradleException("Configured python command doesn't look like python: '$python'")

        return python
    }

    @Option(option = 'user', description = 'Install to the per-user kernel registry.')
    void setUseUserInstallPath(boolean use) {
        if (use)
            this.setKernelInstallPath(this.userInstallPath)
    }

    public final Callable<File> userInstallPath = {
        String python = this.getPythonAndCheckValid()

        String dataDir = this.runCommand("$python -m jupyter --data-dir")
        return project.file(dataDir).absoluteFile
    }

    @Option(option = 'sys-prefix', description = 'Install to Python\'s sys.prefix. Useful in conda/virtual environments.')
    void setUseSysPrefixInstallPath(boolean use) {
        if (use)
            this.setKernelInstallPath(this.sysPrefixInstallPath)
    }

    public final Callable<File> sysPrefixInstallPath = {
        String python = this.getPythonAndCheckValid()

        String prefix = this.runCommand($/$python -c "import sys;print(sys.prefix)"/$)
        return this.prefixInstallPath(prefix).call()
    }

    @Option(option = 'prefix', description = 'Specify a prefix to install to, e.g. an env. The kernelspec will be installed in PREFIX/share/jupyter/kernels/')
    void setUsePrefixInstallPath(String prefix) {
        this.setKernelInstallPath(this.prefixInstallPath(prefix))
    }

    Callable<File> prefixInstallPath(String prefix) {
        return {
            def path = [
                    project.file(prefix).absolutePath,
                    'share',
                    'jupyter',
            ]

            return project.file(path.join(File.separator))
        }
    }

    @Option(option = 'legacy', description = 'Install to $HOME/.ipython. Not recommended but available if needed.')
    void setUseLegacyInstallPath(boolean use) {
        if (use)
            this.setKernelInstallPath(this.legacyInstallPath)
    }

    public final Callable<File> legacyInstallPath = {
        String USER_HOME = System.getProperty('user.home')
        def path = [
                project.file(USER_HOME).absolutePath,
                '.ipython',
        ]

        return project.file(path.join(File.separator))
    }

    @Option(option = 'default', description = 'Install for all users.')
    void setUseDefaultInstallPath(boolean use) {
        if (use)
            this.setKernelInstallPath(this.defaultInstallPath)
    }

    public final Callable<File> defaultInstallPath = {
        String python = this.getPythonAndCheckValid()

        String sysDir = this.runCommand($/$python -c "import jupyter_core.paths as j; print(j.SYSTEM_JUPYTER_PATH[0])"/$)
        return project.file(sysDir).absoluteFile
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
    Callable<File> commandLineSpecifiedPath(Callable<File> fallback = this.defaultInstallPath) {
        return {
            String pathProp = project.findProperty(PropertyNames.INSTALL_KERNEL_PATH)
            if (pathProp == null)
                return fallback.call()

            switch (pathProp) {
                case '@USER@':
                    return this.userInstallPath.call()
                case '@SYS_PREFIX@':
                    return this.sysPrefixInstallPath.call()
                case '@LEGACY@':
                    return this.legacyInstallPath.call()
                default:
                    if (pathProp.trim().isEmpty())
                        return this.defaultInstallPath.call()
                    else
                        return this.prefixInstallPath(pathProp).call()
            }
        }
    }


    @OutputDirectory
    File getKernelDirectory() {
        return new File([this.kernelInstallPath.absolutePath, 'kernels', this.kernelInstallSpec.getKernelName()]
                .join(File.separator))
    }


    @Nested
    KernelJson getKernelSpec() {
        Map<String, String> env = new LinkedHashMap<>(this.kernelInstallSpec.kernelEnv) // copy the default
        Map<String, List<String>> providedParams = this.getProvidedParameters()

        this.kernelParameters.params.each { param ->
            boolean handledSomeParam = false
            Closure<?> handleParamValue = { String value ->
                handledSomeParam = true
                value = param.preProcessAndValidateValue(value)
                param.addValueToEnv(value, env)
            }

            if (param.name in providedParams) {
                providedParams[param.name].each(handleParamValue)
                return
            }

            String value = project.findProperty(PropertyNames.INSTALL_KERNEL_PROP_PREFIX + param.name)
            if (value != null)
                handleParamValue(value)

            // Check for usage with an indexed prop

            // Try index 0
            value = project.findProperty("${PropertyNames.INSTALL_KERNEL_PROP_PREFIX}${param.name}.0")
            if (value != null)
                handleParamValue(value)

            // Try index 1 and continue counting up by 1 to get as many props as passed
            int i = 1
            value = project.findProperty("${PropertyNames.INSTALL_KERNEL_PROP_PREFIX}${param.name}.$i")
            while (value != null) {
                handledSomeParam = true
                handleParamValue(value)
                i++
                value = project.findProperty("${PropertyNames.INSTALL_KERNEL_PROP_PREFIX}${param.name}.$i")
            }

            // If none of the above attempts to find a property worked and the param has a default value,
            // just use the default.
            if (!handledSomeParam && param.defaultValue != null)
                handleParamValue(param.defaultValue)
        }

        return new KernelJson(
                this.getInstalledKernelJar(),
                this.kernelInstallSpec.kernelDisplayName,
                this.kernelInstallSpec.kernelLanguage,
                this.kernelInstallSpec.kernelInterruptMode,
                env,
        )
    }


    @Internal
    private File getInstalledKernelJar() {
        return new File(this.kernelDirectory, this.kernelInstallSpec.kernelExecutable.name)
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        File kernelSpecFile = new File(this.getKernelDirectory(), 'kernel.json')
        kernelSpecFile.text = this.kernelSpec.toString()

        super.project.copy {
            from this.kernelInstallSpec.kernelResources
            from this.kernelInstallSpec.kernelExecutable
            into this.getKernelDirectory()
        }
    }

}
