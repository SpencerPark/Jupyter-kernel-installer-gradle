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

import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.io.File
import javax.inject.Inject

private val KEYWORDS = setOf("user", "sys-prefix", "prefix", "replace", "help")

sealed class KernelParameterSpec @Inject constructor(
        objects: ObjectFactory,
        @Input val name: String,
        @Input val environmentVariable: String
) : WithGradleDslExtensions {
    @Input @Optional val description: Property<String> = objects.property(String::class.java)

    @Input
    val aliases: MapProperty<String, String> = objects.mapProperty(String::class.java, String::class.java).convention(mutableMapOf())
    @Input @Optional val defaultValue: Property<String> = objects.property(String::class.java)

    init {
        if (!environmentVariable.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*")))
            throw GradleException("Environment variable name must match '[a-zA-Z_][a-zA-Z0-9_]*' but was '$environmentVariable'")
        if (!name.matches(Regex("[a-zA-Z][-_a-zA-Z0-9]*")))
            throw GradleException("Name must match '[a-zA-Z][-_a-zA-Z0-9]*' but was '$name'")
        if (name in KEYWORDS)
            throw GradleException("Name cannot be one of $KEYWORDS but was '$name'")
    }

    fun description(description: String?) = this.description.set(description)
    fun aliases(aliases: Map<String, String>) = this.aliases.putAll(aliases)
    fun defaultValue(defaultValue: String?) = this.defaultValue.set(defaultValue)

    open fun preProcessAndValidateValue(value: String): String = aliases.get().getOrDefault(value, value)

    abstract fun addValueToEnv(value: String, env: MutableMap<String, String>)
}


open class StringSpec @Inject constructor(objects: ObjectFactory, name: String, environmentVariable: String) : KernelParameterSpec(objects, name, environmentVariable) {
    override fun addValueToEnv(value: String, env: MutableMap<String, String>) {
        env[super.environmentVariable] = value
    }
}

open class NumberSpec @Inject constructor(objects: ObjectFactory, name: String, environmentVariable: String) : KernelParameterSpec(objects, name, environmentVariable) {
    override fun preProcessAndValidateValue(value: String): String {
        value.toDoubleOrNull() ?: throw GradleException("$name parameter expects a number value but was given '$value'")
        return super.preProcessAndValidateValue(value)
    }

    override fun addValueToEnv(value: String, env: MutableMap<String, String>) {
        env[super.environmentVariable] = value
    }
}

open class ListSpec @Inject constructor(objects: ObjectFactory, name: String, environmentVariable: String) : KernelParameterSpec(objects, name, environmentVariable) {
    @Internal val PATH_SEPARATOR = "\u0000\u0001"
    @Internal val FILE_SEPARATOR = "\u0000\u0002"

    @Input val separator = objects.property(String::class.java).convention(" ")

    fun separator(separator: String?) = this.separator.set(separator)
    fun setSeparator(separator: String?) = this.separator.set(separator)

    fun usePathSeparator() = this.separator.set(PATH_SEPARATOR)
    fun useFileSeparator() = this.separator.set(FILE_SEPARATOR)

    override fun addValueToEnv(value: String, env: MutableMap<String, String>) {
        env.compute(super.environmentVariable) { _, _current ->
            var current = _current ?: return@compute value

            current += when (this.separator.get()) {
                PATH_SEPARATOR -> File.pathSeparator
                FILE_SEPARATOR -> File.separator
                else -> this.separator.get()
            }

            current += value
            return@compute current
        }
    }
}

open class OneOfSpec @Inject constructor(objects: ObjectFactory, name: String, environmentVariable: String) : KernelParameterSpec(objects, name, environmentVariable) {
    @Input val values: ListProperty<String> = objects.listProperty(String::class.java).convention(mutableListOf())

    fun values(values: List<String>) = this.values.addAll(values)
    fun value(value: String) = this.values.add(value)

    override fun preProcessAndValidateValue(value: String): String {
        if (value !in this.values.get())
            throw GradleException("$name parameter expects one of ${this.values.get()} as a value but was given '$value'")

        return super.preProcessAndValidateValue(value)
    }

    override fun addValueToEnv(value: String, env: MutableMap<String, String>) {
        env[super.environmentVariable] = value
    }
}