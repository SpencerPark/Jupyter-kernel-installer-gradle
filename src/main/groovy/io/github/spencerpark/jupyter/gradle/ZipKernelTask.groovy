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
import io.github.spencerpark.jupyter.gradle.installers.InstallersSpec
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Action
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.MapFileTree
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.gradle.util.ConfigureUtil

import static io.github.spencerpark.jupyter.gradle.installers.InstallerMethod.PYTHON_SCRIPT

@CompileStatic
class ZipKernelTask extends Zip {
    private static final String UNSET_PATH_TOKEN = "@KERNEL_INSTALL_DIRECTORY@"
    private static final String KERNEL_JSON_PATH = 'kernel.json'

    private final KernelInstallProperties _kernelInstallProps
    private final CopySpecInternal _kernelJson

    private final InstallersSpec _installers

    ZipKernelTask() {
        this._kernelInstallProps = new KernelInstallProperties(super.getProject())
        this._installers = new InstallersSpec()
        this._installers.with('python')

        // Thanks org/gradle/jvm/tasks/Jar.java for all your help :)
        this._kernelJson = (CopySpecInternal) getMainSpec().addFirst()
        this._kernelJson.addChild().from {
            MapFileTree kernelSource = new MapFileTree(getTemporaryDirFactory(), getFileSystem(), getDirectoryFileTreeFactory())
            kernelSource.add(KERNEL_JSON_PATH, { OutputStream out ->
                //noinspection UnnecessaryQualifiedReference Groovy's resolution at runtime cannot find UNSET_PATH_TOKEN unless qualified
                KernelSpec spec = new KernelSpec(
                        "$ZipKernelTask.UNSET_PATH_TOKEN/${this._kernelInstallProps.getKernelExecutable().getName()}",
                        this._kernelInstallProps.getKernelDisplayName(),
                        this._kernelInstallProps.getKernelLanguage(),
                        this._kernelInstallProps.getKernelInterruptMode(),
                        this._kernelInstallProps.getKernelEnv())

                out.write(spec.toString().getBytes('UTF-8'))
            })
            return new FileTreeAdapter(kernelSource)
        }

        getMainSpec().appendCachingSafeCopyAction { FileCopyDetails details ->
            if (details.getPath().equalsIgnoreCase(KERNEL_JSON_PATH))
                details.exclude()
        }

        getMainSpec().from {
            return this._kernelInstallProps.getKernelResources()
        }
        getMainSpec().from {
            return this._kernelInstallProps.getKernelExecutable()
        }
        getMainSpec().into {
            return this._kernelInstallProps.getKernelName()
        }

        CopySpec installerScriptsSpec = getRootSpec().addChild().into('')
        installerScriptsSpec.from {
            MapFileTree installerScripts = new MapFileTree(getTemporaryDirFactory(), getFileSystem(), getDirectoryFileTreeFactory())

            Closure<Action<OutputStream>> resourceAsFile = { String path ->
                return { OutputStream out ->
                    Reader source = ZipKernelTask.class.getClassLoader().getResourceAsStream(path).newReader()
                    ReplaceTokens filteredSource = new ReplaceTokens(source)
                    ConfigureUtil.configureByMap(filteredSource, tokens: [
                            'KERNEL_NAME'     : this._kernelInstallProps.getKernelName(),
                            'KERNEL_DIRECTORY': this._kernelInstallProps.getKernelName(),
                    ])
                    out << filteredSource
                    source.close()
                } as Action<OutputStream>
            }

            switch (this._installers) {
                case PYTHON_SCRIPT:
                    installerScripts.add('install.py', resourceAsFile('install-scripts/python/install.template.py'))
                    break
            }

            return new FileTreeAdapter(installerScripts)
        }
    }


    @Nested
    KernelInstallProperties getKernelInstallProps() {
        return this._kernelInstallProps
    }

    ZipKernelTask kernelInstallProps(
            @DelegatesTo(value = KernelInstallProperties.class, strategy = Closure.DELEGATE_FIRST) Closure configureClosure) {
        ConfigureUtil.configure(configureClosure, this._kernelInstallProps)
        return this
    }

    ZipKernelTask kernelInstallProps(Action<? super KernelInstallProperties> configureAction) {
        configureAction.execute(this._kernelInstallProps)
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
}
