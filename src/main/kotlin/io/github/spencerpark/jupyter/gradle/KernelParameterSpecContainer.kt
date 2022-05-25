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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Nested
import org.gradle.util.ConfigureUtil

class KernelParameterSpecContainer(private val objects: ObjectFactory) {
    @Nested
    val params: ListProperty<KernelParameterSpec> = objects.listProperty(KernelParameterSpec::class.java).convention(mutableListOf())

    private inline fun <reified T : KernelParameterSpec>spec(name: String, environmentVariable: String) = objects.newInstance(T::class.java, name, environmentVariable)

    fun string(name: String, environmentVariable: String) = params.add(spec<StringSpec>(name, environmentVariable))
    fun string(name: String, environmentVariable: String, configure: Action<in StringSpec>) = this.params.add(spec<StringSpec>(name, environmentVariable).also(configure::execute))
    fun string(name: String, environmentVariable: String, @DelegatesTo(value = StringSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) = this.params.add(ConfigureUtil.configure(configureClosure, spec<StringSpec>(name, environmentVariable)))

    fun number(name: String, environmentVariable: String) = this.params.add(spec<NumberSpec>(name, environmentVariable))
    fun number(name: String, environmentVariable: String, configure: Action<in NumberSpec>) = this.params.add(spec<NumberSpec>(name, environmentVariable).also(configure::execute))
    fun number(name: String, environmentVariable: String, @DelegatesTo(value = NumberSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) = this.params.add(ConfigureUtil.configure(configureClosure, spec<NumberSpec>(name, environmentVariable)))

    fun list(name: String, environmentVariable: String) = this.params.add(spec<ListSpec>(name, environmentVariable))
    fun list(name: String, environmentVariable: String, configure: Action<in ListSpec>) = this.params.add(spec<ListSpec>(name, environmentVariable).also(configure::execute))
    fun list(name: String, environmentVariable: String, @DelegatesTo(value = ListSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) = this.params.add(ConfigureUtil.configure(configureClosure, spec<ListSpec>(name, environmentVariable)))

    fun oneOf(name: String, environmentVariable: String) = this.params.add(spec<OneOfSpec>(name, environmentVariable))
    fun oneOf(name: String, environmentVariable: String, configure: Action<in OneOfSpec>) = this.params.add(spec<OneOfSpec>(name, environmentVariable).also(configure::execute))
    fun oneOf(name: String, environmentVariable: String, @DelegatesTo(value = OneOfSpec::class, strategy = Closure.DELEGATE_FIRST) configureClosure: Closure<*>) = this.params.add(ConfigureUtil.configure(configureClosure, spec<OneOfSpec>(name, environmentVariable)))
}
