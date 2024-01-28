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

class PropertyNames {
    companion object {
        const val INSTALL_KERNEL_PATH = "installKernel.path"
        const val INSTALL_KERNEL_PYTHON = "installKernel.python"

        const val INSTALL_KERNEL_PROP_PREFIX = "kernelParameter."

    }
}

const val PROJECT_EXTENSION_NAME = "jupyter"
const val EXTENSION_BUILD_DIR_PREFIX = "jupyter"
const val TASK_GROUP_NAME = "jupyter"

const val GENERATE_KERNEL_JSON_TASK_NAME = "generateKernelJson"
const val GENERATE_PYTHON_INSTALLER_TASK_NAME = "generatePythonInstaller"
const val STAGE_EXTRA_KERNEL_RESOURCES_TASK_NAME = "stageExtraKernelResources"
const val INSTALL_KERNEL_TASK_NAME = "installKernel"
const val ZIP_KERNEL_TASK_NAME = "zipKernel"