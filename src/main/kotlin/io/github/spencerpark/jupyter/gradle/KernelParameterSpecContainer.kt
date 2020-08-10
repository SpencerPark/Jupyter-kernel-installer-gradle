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
package io.github.spencerpark.jupyter.gradle

import groovy.lang.Closure
import groovy.lang.DelegatesTo
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.util.ConfigureUtil

class KernelParameterSpecContainer(private val project: Project) {
    private val _params = project.objects.listProperty(KernelParameterSpec::class.java).convention(listOf())

    @Nested
    fun getParams(): List<KernelParameterSpec> = this._params.get()
    @Internal
    fun getParamsProvider(): Provider<List<KernelParameterSpec>> = this._params
    fun setParams(params: List<KernelParameterSpec>) = this._params.set(params)
    fun setParams(params: Provider<List<KernelParameterSpec>>) = this._params.set(params)

    fun string(name: String, environmentVariable: String) = this._params.add(StringSpec(project, name, environmentVariable))
    fun string(name: String, environmentVariable: String, configure: Action<in StringSpec>) = this._params.add(StringSpec(project, name, environmentVariable).also(configure::execute))
    fun string(name: String, environmentVariable: String, @DelegatesTo(value = StringSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) = this._params.add(ConfigureUtil.configure(configureClosure, StringSpec(project, name, environmentVariable)))

    fun number(name: String, environmentVariable: String) = this._params.add(NumberSpec(project, name, environmentVariable))
    fun number(name: String, environmentVariable: String, configure: Action<in NumberSpec>) = this._params.add(NumberSpec(project, name, environmentVariable).also(configure::execute))
    fun number(name: String, environmentVariable: String, @DelegatesTo(value = NumberSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) = this._params.add(ConfigureUtil.configure(configureClosure, NumberSpec(project, name, environmentVariable)))

    fun list(name: String, environmentVariable: String) = this._params.add(ListSpec(project, name, environmentVariable))
    fun list(name: String, environmentVariable: String, configure: Action<in ListSpec>) = this._params.add(ListSpec(project, name, environmentVariable).also(configure::execute))
    fun list(name: String, environmentVariable: String, @DelegatesTo(value = ListSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) = this._params.add(ConfigureUtil.configure(configureClosure, ListSpec(project, name, environmentVariable)))

    fun oneOf(name: String, environmentVariable: String) = this._params.add(OneOfSpec(project, name, environmentVariable))
    fun oneOf(name: String, environmentVariable: String, configure: Action<in OneOfSpec>) = this._params.add(OneOfSpec(project, name, environmentVariable).also(configure::execute))
    fun oneOf(name: String, environmentVariable: String, @DelegatesTo(value = OneOfSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) = this._params.add(ConfigureUtil.configure(configureClosure, OneOfSpec(project, name, environmentVariable)))
}
