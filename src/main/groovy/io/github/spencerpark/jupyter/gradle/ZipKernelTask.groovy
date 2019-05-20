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
import io.github.spencerpark.jupyter.gradle.installers.InstallerParameterSpec
import io.github.spencerpark.jupyter.gradle.installers.InstallersSpec
import io.github.spencerpark.jupyter.gradle.installers.PythonScriptGenerator
import io.github.spencerpark.jupyter.gradle.installers.SimpleScriptGenerator
import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.GeneratedSingletonFileTree
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.ConfigureUtil

import static io.github.spencerpark.jupyter.gradle.installers.InstallerMethod.PYTHON_SCRIPT

@CompileStatic
class ZipKernelTask extends Zip {
    /**
     * A placeholder identifier that is added into the kernel.json to be replaced when installed. The
     * path to the kernel is not known until it is installed which is why this field needs to be
     * deferred.
     */
    private static final String UNSET_PATH_TOKEN = "@KERNEL_INSTALL_DIRECTORY@"

    /**
     * The path relative to the zip archive root directory, to the kernel.json file.
     */
    private static final String KERNEL_JSON_PATH = 'kernel.json'

    /**
     * The properties of the kernel being install. These will end up inside the generated kernel.json.
     */
    private final KernelInstallSpec _kernelInstallSpec

    /**
     * A virtual file that is generated before being included in the output zip file. It is the
     * kernel's configuration file that is used by Jupyter to launch the kernel.
     */
    private final CopySpecInternal _kernelJson

    /**
     * A specification of which installers to generate and include in the output zip.
     */
    private final InstallersSpec _installers

    /**
     * Parameters during installation that may be configured.
     */
    private final KernelParameterSpecContainer _kernelParameters

    ZipKernelTask() {
        this._kernelInstallSpec = new KernelInstallSpec(super.project)
        this._installers = new InstallersSpec()
        this._installers.with('python')
        this._kernelParameters = new KernelParameterSpecContainer(super.project)

        // Thanks org/gradle/jvm/tasks/Jar.java for all your help :)
        this._kernelJson = (CopySpecInternal) getMainSpec().addFirst()
        this._kernelJson.addChild().from {
            return this.generatedFileTree(KERNEL_JSON_PATH, { OutputStream out ->
                //noinspection UnnecessaryQualifiedReference Groovy's resolution at runtime cannot find UNSET_PATH_TOKEN unless qualified
                KernelJson spec = new KernelJson(
                        "$ZipKernelTask.UNSET_PATH_TOKEN/${this.kernelInstallSpec.kernelExecutable.asFile.getName()}",
                        this.kernelInstallSpec.kernelDisplayName,
                        this.kernelInstallSpec.kernelLanguage,
                        this.kernelInstallSpec.kernelInterruptMode,
                        this.kernelInstallSpec.kernelEnv)

                out.write(spec.toString().getBytes('UTF-8'))
            })
        }

        getMainSpec().appendCachingSafeCopyAction { FileCopyDetails details ->
            if (details.getPath().equalsIgnoreCase(KERNEL_JSON_PATH))
                details.exclude()
        }

        getMainSpec().from {
            return this.kernelInstallSpec.kernelResources
        }
        getMainSpec().from {
            return this.kernelInstallSpec.kernelExecutable
        }
        getMainSpec().into {
            return this.kernelInstallSpec.kernelName
        }

        CopySpec installerScriptsSpec = getRootSpec().addChild().into('')
        installerScriptsSpec.from {
            switch (this._installers) {
                case PYTHON_SCRIPT:
                    PythonScriptGenerator generator = new PythonScriptGenerator()
                    this.kernelParameters.params.each { generator.addParameter(compileParam(it)) }
                    return this.generatedFileTree('install.py', loadTemplate('install-scripts/python/install.template.py', generator))
                    break
            }

            return project.files()
        }
    }

    private FileTree generatedFileTree(String fileName, Action<OutputStream> generator) {
        GeneratedSingletonFileTree tree = new GeneratedSingletonFileTree(super.getTemporaryDirFactory(), fileName, generator)
        return new FileTreeAdapter(tree)
    }

    private static InstallerParameterSpec compileParam(KernelParameterSpec kSpec) {
        InstallerParameterSpec iSpec = new InstallerParameterSpec(kSpec.name, kSpec.environmentVariable)
        iSpec.description = kSpec.description
        iSpec.defaultValue = kSpec.defaultValue
        iSpec.aliases = kSpec.aliases

        if (kSpec instanceof KernelParameterSpec.StringSpec) {
            iSpec.type = InstallerParameterSpec.Type.STRING
        } else if (kSpec instanceof KernelParameterSpec.NumberSpec) {
            iSpec.type = InstallerParameterSpec.Type.FLOAT
        } else if (kSpec instanceof KernelParameterSpec.ListSpec) {
            iSpec.listSep = kSpec.separator
        } else if (kSpec instanceof KernelParameterSpec.OneOfSpec) {
            iSpec.choices = kSpec.values
        }

        return iSpec
    }

    private Action<OutputStream> loadTemplate(String path, SimpleScriptGenerator generator) {
        return { OutputStream out ->
            String source = ZipKernelTask.class.getClassLoader().getResourceAsStream(path).text

            String name = this.kernelInstallSpec.kernelName
            generator.putToken('KERNEL_NAME', name)
            generator.putToken('KERNEL_DIRECTORY', name)

            out << generator.compile(source)
        } as Action<OutputStream>
    }


    @Option(option = 'archive-name', description = 'Set the name of the output archive.')
    void setArchiveName(String name) {
        super.setArchiveName(name)
    }


    @Nested
    KernelInstallSpec getKernelInstallSpec() {
        return this._kernelInstallSpec
    }

    ZipKernelTask kernelInstallSpec(
            @DelegatesTo(value = KernelInstallSpec.class, strategy = Closure.DELEGATE_FIRST) Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, this._kernelInstallSpec)
        return this
    }

    ZipKernelTask kernelInstallSpec(Action<? super KernelInstallSpec> configureAction) {
        configureAction.execute(this._kernelInstallSpec)
        return this
    }


    @Nested
    InstallersSpec getInstallers() {
        return this._installers
    }

    ZipKernelTask installers(
            @DelegatesTo(value = InstallersSpec.class, strategy = Closure.DELEGATE_FIRST) Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, this._installers)
        return this
    }

    Closure<InstallersSpec> getWithInstaller() {
        return this._installers.&with
    }

    Closure<InstallersSpec> getWithoutInstaller() {
        return this._installers.&without
    }

    @Option(option = 'with', description = 'Include an installer in the zipped bundle.')
    void withInstaller(List<String> installerName) {
        this._installers.with(installerName)
    }

    @Option(option = 'without', description = 'Exclude an installer in the zipped bundle.')
    void withoutInstaller(List<String> installerName) {
        this._installers.without(installerName)
    }


    @Nested
    KernelParameterSpecContainer getKernelParameters() {
        return this._kernelParameters
    }

    ZipKernelTask kernelParameters(
            @DelegatesTo(value = KernelParameterSpecContainer, strategy = Closure.DELEGATE_FIRST) Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, this.getKernelParameters())
        return this
    }

    ZipKernelTask kernelParameters(Action<? extends KernelParameterSpecContainer> configure) {
        configure.execute(this.getKernelParameters())
        return this
    }
}
