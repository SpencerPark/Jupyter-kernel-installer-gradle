import nl.javadude.gradle.plugins.license.LicenseExtension

val kotlinxSerializationVersion by extra("1.6.2")
val kotestVersion by extra("5.8.0")

plugins {
    `java-gradle-plugin`
    `embedded-kotlin`
    id("com.gradle.plugin-publish") version "1.2.1"
    id("maven-publish")
    id("com.github.hierynomus.license") version "0.16.1"
}

group = "io.github.spencerpark"
version = "3.0.0-SNAPSHOT"

configure<LicenseExtension> {
    include("**/*.groovy")
    include("**/*.kt")
    header = rootProject.file("../LICENSE")
    strictCheck = true
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
}

tasks.validatePlugins {
    failOnWarning.set(true)
    enableStricterValidation.set(true)
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

gradlePlugin {
    website.set("https://github.com/SpencerPark/Jupyter-kernel-installer-gradle")
    vcsUrl.set("https://github.com/SpencerPark/Jupyter-kernel-installer-gradle.git")
    plugins {
        create("jupyterKernelInstaller") {
            id = "io.github.spencerpark.jupyter-kernel-installer"
            implementationClass = "io.github.spencerpark.jupyter.gradle.JupyterKernelInstallerPlugin"

            displayName = "Jupyter kernel installer"
            description = "A Jupyter kernel packager and installer."
            tags.set(listOf("jupyter", "kernel", "installer"))
        }
    }
}
