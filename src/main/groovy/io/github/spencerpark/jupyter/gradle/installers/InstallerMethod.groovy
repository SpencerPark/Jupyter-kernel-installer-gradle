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