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

import org.gradle.api.tasks.Input
import java.util.*

class InstallersSpec(
        @Input val installerMethods: MutableSet<InstallerMethod> = EnumSet.noneOf(InstallerMethod::class.java)
) {
    fun isCase(method: InstallerMethod): Boolean = method in installerMethods
    operator fun contains(method: InstallerMethod): Boolean = method in installerMethods

    fun leftShit(vararg methods: Any?): InstallersSpec = this.with(*methods)
    fun with(vararg methods: Any?): InstallersSpec {
        for (method in methods) {
            when (method) {
                is String -> installerMethods.addAll(InstallerMethod.byName(method))
                is InstallerMethod -> installerMethods.add(method)
                is Iterable<*> -> method.forEach { this.with(it); Unit }
                else -> throw IllegalArgumentException("Cannot coerce $method into a method.")
            }
        }

        return this
    }

    fun without(vararg methods: Any?): InstallersSpec {
        for (method in methods) {
            when (method) {
                is String -> installerMethods.removeAll(InstallerMethod.byName(method))
                is InstallerMethod -> installerMethods.remove(method)
                is Iterable<*> -> method.forEach { this.without(it); Unit }
                else -> throw IllegalArgumentException("Cannot coerce $method into a method.")
            }
        }

        return this
    }

    override fun toString() = "InstallersSpec$installerMethods"
}