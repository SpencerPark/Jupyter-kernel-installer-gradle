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
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File
import javax.annotation.Nullable

private val KEYWORDS = setOf("user", "sys-prefix", "prefix", "replace", "help")

sealed class KernelParameterSpec(
        project: Project,
        @Input val name: String,
        @Input val environmentVariable: String
) {
    private val _description = project.objects.property(String::class.java)
    private val _aliases = project.objects.mapProperty(String::class.java, String::class.java).convention(mapOf())
    private val _defaultValue = project.objects.property(String::class.java)

    init {
        if (!environmentVariable.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*")))
            throw GradleException("Environment variable name must match '[a-zA-Z_][a-zA-Z0-9_]*' but was '$environmentVariable'")
        if (!name.matches(Regex("[a-zA-Z][-_a-zA-Z0-9]*")))
            throw GradleException("Name must match '[a-zA-Z][-_a-zA-Z0-9]*' but was '$name'")
        if (name in KEYWORDS)
            throw GradleException("Name cannot be one of $KEYWORDS but was '$name'")
    }

    @Input
    @Optional
    @Nullable
    fun getDescription() = this._description.orNull
    fun setDescription(description: String?) = this._description.set(description)
    fun setDescription(description: Provider<String>) = this._description.set(description)
    fun description(description: String?) = this._description.set(description)

    @Input
    fun getAliases(): Map<String, String> = this._aliases.get()
    fun setAliases(aliases: Map<String, String>) = this._aliases.set(aliases)
    fun setAliases(aliases: Provider<Map<String, String>>) = this._aliases.set(aliases)
    fun aliases(aliases: Map<String, String>) = this._aliases.putAll(aliases)

    @Input
    @Optional
    @Nullable
    fun getDefaultValue() = this._defaultValue.orNull
    fun setDefaultValue(defaultValue: String?) = this._defaultValue.set(defaultValue)
    fun setDefaultValue(defaultValue: Provider<String>) = this._defaultValue.set(defaultValue)
    fun defaultValue(defaultValue: String?) = this._defaultValue.set(defaultValue)

    open fun preProcessAndValidateValue(value: String): String = getAliases().getOrDefault(value, value)

    abstract fun addValueToEnv(value: String, env: MutableMap<String, String>)
}


class StringSpec(project: Project, name: String, environmentVariable: String) : KernelParameterSpec(project, name, environmentVariable) {
    override fun addValueToEnv(value: String, env: MutableMap<String, String>) {
        env[super.environmentVariable] = value
    }
}

class NumberSpec(project: Project, name: String, environmentVariable: String) : KernelParameterSpec(project, name, environmentVariable) {
    override fun preProcessAndValidateValue(value: String): String {
        value.toDoubleOrNull() ?: throw GradleException("$name parameter expects a number value but was given '$value'")
        return super.preProcessAndValidateValue(value)
    }

    override fun addValueToEnv(value: String, env: MutableMap<String, String>) {
        env[super.environmentVariable] = value
    }
}

private const val PATH_SEPARATOR = "\u0000\u0001"
private const val FILE_SEPARATOR = "\u0000\u0002"

class ListSpec(project: Project, name: String, environmentVariable: String) : KernelParameterSpec(project, name, environmentVariable) {
    private val _separator = project.objects.property(String::class.java).convention(" ")

    @Input
    fun getSeparator(): String = this._separator.get()
    fun setSeparator(separator: String) = this._separator.set(separator)
    fun setSeparator(separator: Provider<String>) = this._separator.set(separator)
    fun usePathSeparator() = this._separator.set(PATH_SEPARATOR)
    fun useFileSeparator() = this._separator.set(FILE_SEPARATOR)

    override fun addValueToEnv(value: String, env: MutableMap<String, String>) {
        env.compute(super.environmentVariable) { _, _current ->
            var current = _current ?: return@compute value

            current += when (this.getSeparator()) {
                PATH_SEPARATOR -> File.pathSeparator
                FILE_SEPARATOR -> File.separator
                else -> this.getSeparator()
            }

            current += value
            return@compute current
        }
    }
}

class OneOfSpec(project: Project, name: String, environmentVariable: String) : KernelParameterSpec(project, name, environmentVariable) {
    private val _values = project.objects.listProperty(String::class.java).convention(listOf())

    @Input
    fun getValues(): List<String> = this._values.get()
    fun setValues(values: List<String>) = this._values.set(values)
    fun setValues(values: Provider<List<String>>) = this._values.set(values)
    fun values(values: List<String>) = this._values.addAll(values)
    fun value(value: String) = this._values.add(value)

    override fun preProcessAndValidateValue(value: String): String {
        if (value !in this.getValues())
            throw GradleException("$name parameter expects one of ${this.getValues()} as a value but was given '$value'")

        return super.preProcessAndValidateValue(value)
    }

    override fun addValueToEnv(value: String, env: MutableMap<String, String>) {
        env[super.environmentVariable] = value
    }
}