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
import org.gradle.api.tasks.Input

@CompileStatic
class InstallersSpec {
    private final Set<InstallerMethod> _methods

    InstallersSpec() {
        this._methods = EnumSet.noneOf(InstallerMethod.class)
    }

    @Input
    Set<InstallerMethod> getInstallerMethods() {
        return this._methods
    }

    boolean isCase(InstallerMethod method) {
        return this._methods.contains(method)
    }

    Closure<InstallersSpec> leftShift = this.&with

    InstallersSpec with(String installerName) {
        Set<InstallerMethod> methods = InstallerMethod.byName(installerName)
        this._methods.addAll(methods)
        return this
    }

    InstallersSpec with(InstallerMethod method) {
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
        Set<InstallerMethod> methods = InstallerMethod.byName(installerName)
        this._methods.removeAll(methods)
        return this
    }

    InstallersSpec without(InstallerMethod method) {
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