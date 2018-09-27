# Jupyter kernel installer for Gradle

A [Gradle](https://gradle.org/) plugin for kernel developers to use for packaging and locally installing a [Jupyter kernel](http://jupyter.readthedocs.io/en/latest/projects/kernels.html).

Currently being used by the [IJava](https://github.com/SpencerPark/IJava) kernel.

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
    classpath "gradle.plugin.io.github.spencerpark:jupyter-kernel-installer:2.0.0"
  }
}

apply plugin: "io.github.spencerpark.jupyter-kernel-installer"
```

##### The new way

```gradle
plugins {
  id "io.github.spencerpark.jupyter-kernel-installer" version "2.0.0"
}
```

## Configuration options

These options are shared between all tasks and are general declarations about the kernel being developed. These properties are configured in the `jupyter` extension at the top level of the build script. For example:

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
    kernelParameters {
        list('classpath', 'IJAVA_CLASSPATH') {
            separator = PATH_SEPARATOR
            description = 'A list of files separated by the system's path separator to add to the classpath.'
        }

        list('comp-opts', 'IJAVA_COMPILER_OPTS') {
            separator = ' '
            description = 'A space delimited list of command line options that would be passed to the `javac` command when compiling a project.'
        }

        string('timeout', 'IJAVA_TIMEOUT') {
            aliases NO_TIMEOUT: '-1'
            description = 'A duration specifying a timeout (in milliseconds by default) for a single top level statement.'
        }
    }
}
```

| **property**        | **type**                     | **default**             | **description**                                                                                         |
|---------------------|------------------------------|-------------------------|---------------------------------------------------------------------------------------------------------|
| kernelName          | String                       | `project.name`          | The name of the kernel (the kernel folder)                                                              |
| kernelDisplayName   | String                       | _kernelName_            | The display name of the kernel                                                                          |
| kernelLanguage      | String                       | _kernelName_            | The name of the language that the kernel can execute code in                                            |
| kernelInterruptMode | String                       | `'signal'`              | The desired interrupt mode (`'message'` or `'signal'`)                                                  |
| kernelEnv           | Map<String, String>          | `[:]`                   | Environment variable names and values that a kernel may use for configuration                           |
| kernelExecutable    | File                         | `jar.archivePath`       | The build output that must be invoked to start the kernel                                               |
| kernelResources     | FileCollection               | `kernel` directory      | The resources that should be included with the kernel such as a `kernel.js`                             |
| kernelParameters    | KernelParameterSpecContainer | _empty container_       | A declaration of the configurable parameters for generating an install script or installing with gradle |

### `kernelParameters` in more detail

This property (usually configured by a closure rather than set as in the example above) is used to specify parameters that a user installing the kernel could configure for their installation. These manifest themselves as entries to the `kernelEnv` but are dynamic rather than static with respect to the build process.

When generating an installer, these parameters will manifest themselves as command line options (or similar) which the end user can give to configure their installation. See the [zipkernel](#zipkernel) section for more information about generating installers.

When running `installKernel`, these parameters may be set via gradle properties making them accessible from the command line. See the [installKernel](#installkernel) section for more information about passing parameters to this task.

**Parameter declarations** are fairly straight forward. There are 4 types currently supported `string`, `number`, `list`, and `oneOf`. All parameters support some basic properties:

| **property**        | **type**            | **default**                | **description**                                                                                                                                                                                   |
|---------------------|---------------------|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| name                | String              | _**required**, no default_ | The command line name of this property. Must be a valid identifier (matches `[a-zA-Z][-_a-zA-Z0-9]*`). Cannot be any of the reserved names: `['user', 'sys-prefix', 'prefix', 'replace', 'help']` |
| environmentVariable | String              | _**required**, no default_ | The `kernelEnv` variable name to store the value under. Must be a valid environment variable name (matches `[a-zA-Z_][a-zA-Z0-9_]*`).                                                             |
| description         | String              | `null`                     | A human readable description for help menus.                                                                                                                                                      |
| aliases             | Map<String, String> | `[:]`                      | A map of **value aliases**. When a **value** matches an `aliases` key, the value is replaced with the mapping for that alias.                                                                     |
| defaultValue        | String              | `null`                     | A value to set in the `kernelEnv` if no value is given for this parameter during configuration.                                                                                                   |

Additionally, there is an extra property for a `list` parameter:

| **property** | **type** | **default** | **description**                                                                    |
|--------------|----------|-------------|------------------------------------------------------------------------------------|
| separator    | String   | `' '`       | The separator string to use when joining multiple values given for this parameter. |

**Note** the separator value has 2 special setters `usePathSeparator()` and `useFileSeparator()` for platform specific separators when using lists of files.

Also, `oneOf` parameters have an additional property:

| **property** | **type**     | **default** | **description**                                                                          |
|--------------|--------------|-------------|------------------------------------------------------------------------------------------|
| values       | List<String> | `[]`        | A list of valid values for this parameter. Used mainly in configuration of enumerations. |

**Note** use the `values(List<String>)` and `value(String)` functions to _add_ to the values rather than setting them.

## Tasks

Try to use the `jupyter` extension defined above as it will configure all tasks and only use the below options when the configuration must be specific to that task.

### `installKernel`
>   Locally install the kernel.

This task installs the kernel on the machine running gradle. It is useful when building during development or installing from source. It can be configured via the `installKernel` closure or with gradle properties passed from the command line with `-Pname=value`.

| **property**      | **type**                     | **default**                                    | **description**                                                                                  |
|-------------------|------------------------------|------------------------------------------------|--------------------------------------------------------------------------------------------------|
| kernelInstallSpec | KernelInstallSpec            | _inherited from `jupyter`_                     | The kernel specifications including the `kernelDisplayName`, `kernelEnv`, `kernelResources` etc. |
| kernelParameters  | KernelParameterSpecContainer | _inherited from `jupyter`_                     | The parameters that may be configured during installation.                                       |
| pythonExecutable  | String                       | `null`                                         | The python command to use when searching for install locations.                                  |
| kernelInstallPath | File                         | `commandLineSpecifiedPath(defaultInstallPath)` | The installation directory. There are several helper functions for specifying this location.     |

There are several gradle properties (`-Pname=value`) that configure this task just in time.

*   `-PinstallKernel.python=VALUE` sets the `pythonExecutable` to _VALUE_.
*   `-PinstallKernel.path=VALUE` sets the `kernelInstallPath` **if used in conjunction with the `commandLineSpecifiedPath` function** which is the default for the task. There are some special values that take affect:
    *   `@USER@` uses `userInstallPath()` (see `jupyter kernelspec install --user`).
    *   `@SYS_PREFIX@` uses `sysPrefixInstallPath()` (see `jupyter kernelspec install --sys-prefix`).
    *   `@LEGACY@` uses `legacyInstallPath()` (IPython's `$HOME/.ipython` convention).
    *   _empty string_ uses `defaultInstallPath()` (see `jupyter kernelspec install`).
    *   anything else uses `prefixInstallPath(VALUE)` (see `jupyter kernelspec install --prefix=VALUE`).
    *   _unset_ (not declared at all) uses the fallback value from `commandLineSpecifiedPath` which defaults to .`defaultInstallPath`
*   `-PkernelParameter.NAME=VALUE` sets the parameter with name _NAME_ to value _VALUE_.
   *   To specify a list, append `.1` to the first value (e.g. `-PkernelParameter.NAME.1=VALUE_1`) and keep counting up until all the values are specified. **Note:** do not skip any indexes and start at either `0` or `1`, which ever is preferred.

### `zipKernel`
>   Create a zip with the kernel files.

This archiving task creates a ready to distribute bundle of the kernel. It **extends `Zip`** and therefore has all of the configurations options that `Zip` has including `archiveName`, `zip64`, `entryCompression`, etc.).

| **property**      | **type**                     | **default**                | **description**                                                                                        |
|-------------------|------------------------------|----------------------------|--------------------------------------------------------------------------------------------------------|
| kernelInstallSpec | KernelInstallSpec            | _inherited from `jupyter`_ | The kernel specifications including the `kernelDisplayName`, `kernelEnv`, `kernelResources` etc.       |
| kernelParameters  | KernelParameterSpecContainer | _inherited from `jupyter`_ | The parameters that may be configured during installation. These affect the generated install scripts. |
| installers        | InstallersSpec               | `with 'python'`            | A set of installers to generate and include in the archive.                                            |

Currently supported installers include:

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
