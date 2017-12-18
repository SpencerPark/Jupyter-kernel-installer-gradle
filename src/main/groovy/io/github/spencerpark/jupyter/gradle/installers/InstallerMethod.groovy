package io.github.spencerpark.jupyter.gradle.installers

import groovy.transform.CompileStatic
import org.gradle.api.GradleException

@CompileStatic
enum InstallerMethod {
    PYTHON_SCRIPT('python')

    private static Map<String, InstallerMethod> BY_NAME
    private static Map<String, Set<InstallerMethod>> GROUPS_BY_NAME

    InstallerMethod(String... groups) {
        if (BY_NAME == null) BY_NAME = new HashMap<>()
        if (GROUPS_BY_NAME == null) GROUPS_BY_NAME = new HashMap<>()

        BY_NAME.put(this.name().toLowerCase().replace('_', ''), this)
        for (String groupName in groups) {
            GROUPS_BY_NAME.compute(groupName, { String name, Set<InstallerMethod> group ->
                group = group ?: new HashSet<InstallerMethod>()
                group.add(this)
                return group
            })
        }
    }

    boolean isCase(InstallersSpec installers) {
        return this in installers
    }

    static Set<InstallerMethod> byName(String name) {
        String collapsedName = name.replaceAll(~/[-_ \t]/, '')
        InstallerMethod method = BY_NAME.get(collapsedName)
        if (method != null) return Collections.singleton(method)

        Set<InstallerMethod> group = GROUPS_BY_NAME.get(collapsedName)
        if (group != null) return group

        throw new GradleException("No installer method with name '$name'")
    }
}