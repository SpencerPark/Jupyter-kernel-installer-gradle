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
package io.github.spencerpark.jupyter.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.util.ConfigureUtil

@CompileStatic
class KernelParameterSpecContainer {
    private final Project project
    private final PropertyState<List<KernelParameterSpec>> _params

    KernelParameterSpecContainer(Project project) {
        this.project = project
        this._params = (project.property(List) as PropertyState<List<KernelParameterSpec>>)
        this._params.set([])
    }

    @Nested
    List<KernelParameterSpec> getParams() {
        return this._params.get()
    }

    Provider<List<KernelParameterSpec>> getParamsProvider() {
        return this._params
    }

    void setParams(List<KernelParameterSpec> params) {
        this._params.set(params)
    }

    void setParams(Provider<List<KernelParameterSpec>> params) {
        this._params.set(params)
    }

    void string(String name, String environmentVariable,
                @DelegatesTo(value = KernelParameterSpec.StringSpec, strategy = Closure.DELEGATE_FIRST) Closure configClosure) {
        KernelParameterSpec.StringSpec spec = new KernelParameterSpec.StringSpec(this.project, name, environmentVariable)
        ConfigureUtil.configure(configClosure, spec)
        this.params.add(spec)
    }

    void string(String name, String environmentVariable, Action<? super KernelParameterSpec.StringSpec> configure) {
        KernelParameterSpec.StringSpec spec = new KernelParameterSpec.StringSpec(this.project, name, environmentVariable)
        configure.execute(spec)
        this.params.add(spec)
    }

    void string(String name, String environmentVariable) {
        KernelParameterSpec.StringSpec spec = new KernelParameterSpec.StringSpec(this.project, name, environmentVariable)
        this.params.add(spec)
    }

    void number(String name, String environmentVariable,
                @DelegatesTo(value = KernelParameterSpec.NumberSpec, strategy = Closure.DELEGATE_FIRST) Closure configClosure) {
        KernelParameterSpec.NumberSpec spec = new KernelParameterSpec.NumberSpec(this.project, name, environmentVariable)
        ConfigureUtil.configure(configClosure, spec)
        this.params.add(spec)
    }

    void number(String name, String environmentVariable, Action<? super KernelParameterSpec.NumberSpec> configure) {
        KernelParameterSpec.NumberSpec spec = new KernelParameterSpec.NumberSpec(this.project, name, environmentVariable)
        configure.execute(spec)
        this.params.add(spec)
    }

    void number(String name, String environmentVariable) {
        KernelParameterSpec.NumberSpec spec = new KernelParameterSpec.NumberSpec(this.project, name, environmentVariable)
        this.params.add(spec)
    }

    void list(String name, String environmentVariable,
              @DelegatesTo(value = KernelParameterSpec.ListSpec, strategy = Closure.DELEGATE_FIRST) Closure configClosure) {
        KernelParameterSpec.ListSpec spec = new KernelParameterSpec.ListSpec(this.project, name, environmentVariable)
        ConfigureUtil.configure(configClosure, spec)
        this.params.add(spec)
    }

    void list(String name, String environmentVariable, Action<? super KernelParameterSpec.ListSpec> configure) {
        KernelParameterSpec.ListSpec spec = new KernelParameterSpec.ListSpec(this.project, name, environmentVariable)
        configure.execute(spec)
        this.params.add(spec)
    }

    void list(String name, String environmentVariable) {
        KernelParameterSpec.ListSpec spec = new KernelParameterSpec.ListSpec(this.project, name, environmentVariable)
        this.params.add(spec)
    }

    void oneOf(String name, String environmentVariable,
               @DelegatesTo(value = KernelParameterSpec.OneOfSpec, strategy = Closure.DELEGATE_FIRST) Closure configClosure) {
        KernelParameterSpec.OneOfSpec spec = new KernelParameterSpec.OneOfSpec(this.project, name, environmentVariable)
        ConfigureUtil.configure(configClosure, spec)
        this.params.add(spec)
    }

    void oneOf(String name, String environmentVariable, Action<? super KernelParameterSpec.OneOfSpec> configure) {
        KernelParameterSpec.OneOfSpec spec = new KernelParameterSpec.OneOfSpec(this.project, name, environmentVariable)
        configure.execute(spec)
        this.params.add(spec)
    }

    void oneOf(String name, String environmentVariable) {
        KernelParameterSpec.OneOfSpec spec = new KernelParameterSpec.OneOfSpec(this.project, name, environmentVariable)
        this.params.add(spec)
    }
}
