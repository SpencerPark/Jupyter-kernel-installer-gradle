# Jupyter kernel installer for Gradle

A [Gradle](https://gradle.org/) plugin for kernel developers to use for packaging and locally installing a [Jupyter kernel](http://jupyter.readthedocs.io/en/latest/projects/kernels.html).

### Jupyter kernel development on the JVM

This plugin was developed in support of the [Jupyter JVM base kernel project](https://github.com/SpencerPark/jupyter-jvm-basekernel) for creating Jupyter kernels that run on the JVM.

That project has an example kernel implementation (including usage with this plugin) and is a great resource for creating Jupyter kernels for JVM based languages.

### Using the plugin

**Requires Gradle `>=4.0`**

Add the following block to the project's `build.gradle`

##### The old way

```gradle
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.io.github.spencerpark:jupyter-kernel-installer:1.1.7"
  }
}

apply plugin: "io.github.spencerpark.jupyter-kernel-installer"
```

##### The new way

```gradle
plugins {
  id "io.github.spencerpark.jupyter-kernel-installer" version "1.1.7"
}
```

## Configuration options

| **property**        | **type**            | **default**             | **description**                                                                    |
|---------------------|---------------------|-------------------------|------------------------------------------------------------------------------------|
| kernelName          | String              | `project.name`          | The name of the kernel (the kernel folder)                                         |
| kernelDisplayName   | String              | _kernelName_            | The display name of the kernel                                                     |
| kernelLanguage      | String              | _kernelName_            | The name of the language that the kernel can execute code in                       |
| kernelInterruptMode | String              | `'signal'`              | The desired interrupt mode (`'message'` or `'signal'`)                             |
| kernelEnv           | Map<String, String> | `[:]`                   | Environment variable names and values that a kernel may use for configuration      |
| kernelExecutable    | File                | `jar.archivePath`       | The build output that must be invoked to start the kernel                          |
| kernelResources     | FileCollection      | `kernel` directory      | The resources that should be included with the kernel such as a `kernel.js`        |
| kernelInstallPath   | File                | `"$USER_HOME/.ipython"` | The path to a Jupyter data path directory                                          |

These properties are configured in the `jupyter` extension at the top level of the build script.

```gradle
jupyter {
    kernelName = 'Java'
    kernelDisplayName = 'My Java Kernel'
    kernelLanguage = 'java'
    kernelInterruptMode = 'message'
    kernelEnv (
       ENV_VAR_NAME: 'Value',
       OTHER_VAR: 'Other value'
    )
    kernelResources = files('kernel')
}
```

## Tasks

Try to use the `jupyter` extension defined above as it will configure all tasks and only use the below options when the configuration must be local.

### `installKernel`
>   Locally install the kernel.

**Options:**

*   `kernelInstallProps` configure the kernel properties (`kernelDisplayName`, `kernelLanguage`, `kernelEnv`, `kernelExecutable`, `kernelResources`) locally so they only affect this task.
*   `kernelInstallPath` set the install path from the default `"$USER_HOME/.ipython"` for this task

### `zipKernel`
>   Create a zip with the kernel files.

**Options:**

*   `kernelInstallProps` configure the kernel properties (`kernelDisplayName`, `kernelLanguage`, `kernelEnv`, `kernelExecutable`, `kernelResources`) locally so they only affect this task.
*   All options defined for the base `Zip` tasks (such as `archiveName`, `zip64`, `entryCompression`, etc.)
*   `installers` configure the installers distributed with the archive. Defaults to `'python'`.
    *   Currently supported:

        | Name            | Groups     |                        |
        |-----------------|------------| -----------------------|
        | `PYTHON_SCRIPT` | `'python'` | `python install.py -h` |
    *   Use the `with` or `without` method to add or remove a specific installer (`'PYTHON_SCRIPT'`) or group of installers (`'python'`)
    *   ```gradle
        zipKernel {
            installers {
                with 'python'
            }
        }
        ```
