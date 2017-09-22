# Jupyter kernel installer for Gradle

A [Gradle](https://gradle.org/) plugin for kernel developers to use for packaging and locally installing a [Jupyter kernel](http://jupyter.readthedocs.io/en/latest/projects/kernels.html).

### Jupyter kernel development on the JVM

This plugin was developed in support of the [Jupyter JVM base kernel project](https://github.com/SpencerPark/jupyter-jvm-basekernel) for creating Jupyter kernels that run on the JVM.

That project has an example kernel implementation (including usage with this plugin) and is a great resource for creating Jupyter kernels for JVM based languages.

### Using the plugin

Add the following block to the project's `build.gradle`

##### Gradle < 2.1

```gradle
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.io.github.spencerpark:jupyter-kernel-installer:1.0.0"
  }
}

apply plugin: "io.github.spencerpark.jupyter-kernel-installer"
```

##### Gradle >= 2.1

```gradle
plugins {
  id "io.github.spencerpark.jupyter-kernel-installer" version "1.0.0"
}
```

##### Configure the kernelspec

| **property**      | **type**       | **default**             | **description**                                                                         |
|-------------------|----------------|-------------------------|-----------------------------------------------------------------------------------------|
| kernelDisplayName | String         | `project.name`          | The display name of the kernel                                                          |
| kernelLanguage    | String         | `project.name`          | The name of the language that the kernel can execute code in                            |
| kernelExecutable  | File           | `jar.archivePath`       | The build output that must be invoked to start the kernel                               |
| kernelResources   | FileCollection | `kernel` directory      | The resources that should be included with the kernel such as `kernel.js` or icon files |
| kernelInstallPath | File           | `"$USER_HOME/.ipython"` | The path to a Jupyter data path directory                                               |

These properties are configured in the `jupyter` extension.

```gradle
jupyter {
    kernelDisplayName = 'My Java Kernel'
    kernelLanguage = 'java'
}
```

### Run the installer

The plugin includes an install task called `installKernel` which may be executed via

```bash
gradle installKernel
```
