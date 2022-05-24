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
package io.github.spencerpark.jupyter.gradle.installers

import org.gradle.api.GradleException
import java.util.*

enum class InstallerMethod(vararg val groups: String) {
    PYTHON_SCRIPT("python");

    companion object {
        private val BY_NAME = values()
                .map { it.name.lowercase(Locale.ENGLISH).replace("_", "") to it }
                .toMap()
        private val GROUPS_BY_NAME: Map<String, Set<InstallerMethod>> = values()
                .flatMap { m -> m.groups.map { it to m } }
                .groupingBy { it.first }
                .aggregate { _, acc, elem, first ->
                    if (first) setOf(elem.second) else acc!!.union(setOf(elem.second))
                }

        fun byName(name: String): Set<InstallerMethod> {
            val collapsedName = name.replace(Regex("[-_ \t]"), "")
            val method = BY_NAME[collapsedName]
            if (method != null)
                return Collections.singleton(method)

            val group = GROUPS_BY_NAME[collapsedName]
            if (group != null)
                return group

            throw GradleException("No installer method with name '$name'")
        }
    }

    fun isCase(installers: InstallersSpec) = this in installers
}