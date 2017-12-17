package io.github.spencerpark.jupyter.gradle

import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input

@CompileStatic
enum Methods {
    PYTHON_SCRIPT('python')

    private static Map<String, Methods> BY_NAME
    private static Map<String, Set<Methods>> GROUPS_BY_NAME

    Methods(String... groups) {
        if (BY_NAME == null) BY_NAME = new HashMap<>()
        if (GROUPS_BY_NAME == null) GROUPS_BY_NAME = new HashMap<>()

        BY_NAME.put(this.name().toLowerCase().replace('_', ''), this)
        for (String groupName in groups) {
            GROUPS_BY_NAME.compute(groupName, { String name, Set<Methods> group ->
                group = group ?: new HashSet<Methods>()
                group.add(this)
                return group
            })
        }
    }

    boolean isCase(InstallersSpec installers) {
        return this in installers
    }

    static Set<Methods> byName(String name) {
        String collapsedName = name.replaceAll(~/[-_ \t]/, '')
        Methods method = BY_NAME.get(collapsedName)
        if (method != null) return Collections.singleton(method)

        Set<Methods> group = GROUPS_BY_NAME.get(collapsedName)
        if (group != null) return group

        throw new GradleException("No installer method with name '$name'")
    }
}

@CompileStatic
class InstallersSpec {
    private final Set<Methods> _methods

    InstallersSpec() {
        this._methods = EnumSet.noneOf(Methods.class)
    }

    @Input
    Set<Methods> getInstallerMethods() {
        return this._methods
    }

    boolean isCase(Methods method) {
        return this._methods.contains(method)
    }

    Closure<InstallersSpec> leftShift = this.&with

    InstallersSpec with(String installerName) {
        Set<Methods> methods = Methods.byName(installerName)
        this._methods.addAll(methods)
        return this
    }

    InstallersSpec with(Methods method) {
        this._methods.add(method)
        return this
    }

    InstallersSpec with(Object... installers) {
        installers.each(this.&with)
        return this
    }

    InstallersSpec with(List installers) {
        installers.each(this.&with)
        return this
    }

    InstallersSpec without(String installerName) {
        Set<Methods> methods = Methods.byName(installerName)
        this._methods.removeAll(methods)
        return this
    }

    InstallersSpec without(Methods method) {
        this._methods.remove(method)
        return this
    }

    InstallersSpec without(Object... installers) {
        installers.each(this.&without)
        return this
    }

    InstallersSpec without(List installers) {
        installers.each(this.&without)
        return this
    }

    @Override
    String toString() {
        return "InstallersSpec$_methods"
    }
}