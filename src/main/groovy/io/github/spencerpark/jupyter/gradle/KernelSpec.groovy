package io.github.spencerpark.jupyter.gradle

import groovy.text.SimpleTemplateEngine
import groovy.text.TemplateEngine

class KernelSpec {
    private static final String KERNELSPEC_TEMPLATE_PATH = '/kernel.json.template'

    private final String compiledSpec

    private final File installedKernelJar
    private final String kernelDisplayName
    private final String kernelLanguage

    KernelSpec(File installedKernelJar, String kernelDisplayName, String kernelLanguage) {
        this.installedKernelJar = installedKernelJar
        this.kernelDisplayName = kernelDisplayName
        this.kernelLanguage = kernelLanguage

        String compiledSpec = ""
        TemplateEngine templateEngine = new SimpleTemplateEngine()
        InstallKernelTask.class.getResourceAsStream(KERNELSPEC_TEMPLATE_PATH).withReader('UTF-8') {
            template ->
                compiledSpec = templateEngine.createTemplate(template).make(
                        KERNEL_JAR_PATH: getInstalledKernelJar().absolutePath.toString().replace(File.separatorChar, '/' as char),
                        KERNEL_DISPLAY_NAME: getKernelDisplayName(),
                        KERNEL_LANGUAGE: getKernelLanguage()
                )
        }

        this.compiledSpec = compiledSpec
    }

    File getInstalledKernelJar() {
        return installedKernelJar
    }

    String getKernelDisplayName() {
        return kernelDisplayName
    }

    String getKernelLanguage() {
        return kernelLanguage
    }

    @Override
    String toString() {
        return compiledSpec
    }
}
