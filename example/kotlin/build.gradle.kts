import io.github.spencerpark.jupyter.gradle.tasks.GenerateKernelJsonTask
import io.github.spencerpark.jupyter.gradle.tasks.InstallKernelTask
import io.github.spencerpark.jupyter.gradle.tasks.ZipKernelTask

plugins {
    id("java")
    id("io.github.spencerpark.jupyter-kernel-installer")
}

jupyter {
    kernelName.set("kotlin")
    kernelDisplayName("Kotlin DSL Example")
    kernelLanguage.set("kotlin")
    kernelInterruptMode.set("message")

    kernelEnv["IN_JUPYTER_KERNEL"] = "1"

    kernelResources {
        from("kernel") // This is the default if `kernelResources` is not specified

        from(files("example-resource.txt", "example-excluded-resource.txt")) {
            exclude("example-excluded-resource.txt")
            into("extras")
        }
    }

    kernelParameters {
        string("startup-script", "STARTUP_SCRIPT") {
            description.set("A raw code snippet to execute when the kernel starts.")
        }

        number("timeout", "TIMEOUT") {
            aliases["NEVER"] = "-1"
            description by """
                |A timeout in milliseconds for something that I guess takes
                |a while.
                |
                |When less than 1, there is no timeout. If desired, use the
                |constant `NEVER` (--timeout=NEVER) to specify that.
            """.trimMargin("|")
        }

        list("classpath", "CLASSPATH") {
            usePathSeparator()
            description by "A file path separator delimited list of classpath entries."
        }

        list("comp-opts", "EXTRA_COMPILER_OPTS") {
            separator.set(" ")
            description by "A space delimited list of command line options that would be passed to the `javac` command when compiling a project."
        }

        list("startup-scripts-path", "STARTUP_SCRIPTS_PATH") {
            separator.set(PATH_SEPARATOR)
            description by "A file path separator delimited list of scripts to run on startup."
        }

        oneOf("engine", "ENGINE") {
            values += "ENGINE_1"
            values += "ENGINE_2"
            defaultValue.set("ENGINE_1")

            description by "Specify the engine, only 2 options."
        }
    }
}

tasks.named<GenerateKernelJsonTask>("generateKernelJson") {
    kernelInstallSpec.kernelDisplayName by "${kernelInstallSpec.kernelDisplayName.get()} Renamed Slightly"
}

tasks.named<InstallKernelTask>("installKernel") {
    kernelInstallPath by commandLineSpecifiedPathOr(
        project.layout.buildDirectory.dir("jupyter/mock-install")
    )
}

tasks.named<ZipKernelTask>("zipKernel") {
    withInstaller("python")

    archiveBaseName = "kotlin-dsl-example-kernel-dist"
}