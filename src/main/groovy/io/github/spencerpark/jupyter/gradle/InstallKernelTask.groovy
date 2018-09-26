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
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.util.ConfigureUtil

import java.util.concurrent.Callable

@CompileStatic
class InstallKernelTask extends DefaultTask {
    private final KernelInstallSpec _kernelInstallSpec
    private final PropertyState<String> _pythonExecutable
    private final PropertyState<File> _kernelInstallPath

    InstallKernelTask() {
        this._kernelInstallSpec = new KernelInstallSpec(super.project)
        this._pythonExecutable = super.project.property(String.class)

        this._kernelInstallPath = super.project.property(File.class)
        this._kernelInstallPath.set(project.provider(this.defaultInstallPath))
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


    @Optional
    @Input
    String getPythonExecutable() {
        return project.findProperty(PropertyNames.INSTALL_KERNEL_PYTHON) ?: this._pythonExecutable.getOrNull()
    }

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

    public final Callable<File> userInstallPath = {
        String python = this.getPythonAndCheckValid()

        String dataDir = this.runCommand("$python -m jupyter --data-dir")
        return project.file(dataDir).absoluteFile
    }

    public final Callable<File> sysPrefixInstallPath = {
        String python = this.getPythonAndCheckValid()

        String prefix = this.runCommand($/$python -c "import sys;print(sys.prefix)"/$)
        return this.prefixInstallPath(prefix).call()
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

    public final Callable<File> legacyInstallPath = {
        String USER_HOME = System.getProperty('user.home')
        def path = [
                project.file(USER_HOME).absolutePath,
                '.ipython',
        ]

        return project.file(path.join(File.separator))
    }

    public final Callable<File> defaultInstallPath = {
        String python = this.getPythonAndCheckValid()

        String sysDir = runCommand($/$python -c "import jupyter_core.paths as j; print(j.SYSTEM_JUPYTER_PATH[0])"/$)
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


    @Internal
    File getInstalledKernelJar() {
        return new File(this.kernelDirectory, this.kernelInstallSpec.kernelExecutable.name)
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        this.writeKernelSpec()
        super.project.copy {
            from this.kernelInstallSpec.kernelResources
            from this.kernelInstallSpec.kernelExecutable
            into this.getKernelDirectory()
        }
    }

    private void writeKernelSpec() {
        KernelJson spec = new KernelJson(
                this.getInstalledKernelJar(),
                this.kernelInstallSpec.kernelDisplayName,
                this.kernelInstallSpec.kernelLanguage,
                this.kernelInstallSpec.kernelInterruptMode,
                this.kernelInstallSpec.kernelEnv)

        File kernelSpec = new File(this.getKernelDirectory(), 'kernel.json')
        kernelSpec.text = spec.toString()
    }

}
