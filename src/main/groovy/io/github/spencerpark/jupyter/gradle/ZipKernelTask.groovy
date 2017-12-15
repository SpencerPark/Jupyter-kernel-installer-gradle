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
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.internal.file.collections.MapFileTree
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip

@CompileStatic
class ZipKernelTask extends Zip {
    private static final String UNSET_PATH_TOKEN = "@kernel_install_directory@"
    private static final String KERNEL_JSON_PATH = 'kernel.json'

    private final KernelInstallProperties _kernelInstallProps
    private final CopySpecInternal _kernelJson

    ZipKernelTask() {
        this._kernelInstallProps = new KernelInstallProperties(super.getProject())

        // Thanks org/gradle/jvm/tasks/Jar.java for all your help :)
        this._kernelJson = (CopySpecInternal) getRootSpec().addFirst()
        this._kernelJson.addChild().from {
            MapFileTree kernelSource = new MapFileTree(getTemporaryDirFactory(), getFileSystem(), getDirectoryFileTreeFactory())
            kernelSource.add(KERNEL_JSON_PATH, { OutputStream out ->
                //noinspection UnnecessaryQualifiedReference Groovy's resolution at runtime cannot find UNSET_PATH_TOKEN unless qualified
                KernelSpec spec = new KernelSpec(
                        "$ZipKernelTask.UNSET_PATH_TOKEN/${this._kernelInstallProps.getKernelExecutable().getName()}",
                        this._kernelInstallProps.getKernelDisplayName(),
                        this._kernelInstallProps.getKernelLanguage(),
                        this._kernelInstallProps.getKernelEnv())

                out.write(spec.toString().getBytes('UTF-8'))
            })
            return new FileTreeAdapter(kernelSource)
        }
        getMainSpec().appendCachingSafeCopyAction { FileCopyDetails details ->
            if (details.getPath().equalsIgnoreCase(KERNEL_JSON_PATH))
                details.exclude()
        }

        from {
            return this._kernelInstallProps.getKernelResources()
        }
        from {
            return this._kernelInstallProps.getKernelExecutable()
        }
    }

    @Nested
    KernelInstallProperties getKernelInstallProps() {
        return this._kernelInstallProps
    }

    void kernelInstallProps(Action<KernelInstallProperties> configure) {
        configure.execute(this._kernelInstallProps)
    }
}
