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
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input

@CompileStatic
abstract class KernelParameterSpec {
    private static final Set<String> KEYWORDS = ['user', 'sys-prefix', 'prefix', 'replace', 'help'] as Set<String>

    static class StringSpec extends KernelParameterSpec {
        StringSpec(Project project, String name, String environmentVariable) {
            super(project, name, environmentVariable)
        }
    }

    static class NumberSpec extends KernelParameterSpec {
        NumberSpec(Project project, String name, String environmentVariable) {
            super(project, name, environmentVariable)
        }
    }

    static class ListSpec extends KernelParameterSpec {
        private final String PATH_SEPARATOR = '\0\1'
        private final String FILE_SEPARATOR = '\0\2'

        private final PropertyState<String> _separator

        ListSpec(Project project, String name, String environmentVariable) {
            super(project, name, environmentVariable)
            this._separator = project.property(String)
            this._separator.set(' ')
        }

        @Input
        String getSeparator() {
            return this._separator.get()
        }

        void setSeparator(String separator) {
            this._separator.set(separator)
        }

        void setSeparator(Provider<String> separator) {
            this._separator.set(separator)
        }

        void usePathSeparator() {
            this._separator.set(PATH_SEPARATOR)
        }

        void useFileSeparator() {
            this._separator.set(FILE_SEPARATOR)
        }
    }

    static class OneOfSpec extends KernelParameterSpec {
        private final PropertyState<List<String>> _values

        OneOfSpec(Project project, String name, String environmentVariable) {
            super(project, name, environmentVariable)
            this._values = (project.property(List) as PropertyState<List<String>>)
            this._values.set([])
        }

        @Input
        List<String> getValues() {
            return this._values.get()
        }

        void setValues(List<String> values) {
            this._values.set(values)
        }

        void setValues(Provider<List<String>> values) {
            this._values.set(values)
        }

        void values(List<String> values) {
            this._values.get().addAll(values)
        }

        void value(String value) {
            this._values.get().add(value)
        }
    }

    @Input
    public final String name

    @Input
    public final String environmentVariable

    private final PropertyState<String> _description

    private final PropertyState<Map<String, String>> _aliases

    private final PropertyState<String> _defaultValue

    KernelParameterSpec(Project project, String name, String environmentVariable) {
        this.name = name
        this.environmentVariable = environmentVariable

        this._description = project.property(String)
        this._aliases = (project.property(Map) as PropertyState<Map<String, String>>)
        this._aliases.set([:])
        this._defaultValue = project.property(String)

        if (!environmentVariable.matches(/[a-zA-Z_][a-zA-Z0-9_]*/))
            throw new GradleException("Environment variable name must match '[a-zA-Z_][a-zA-Z0-9_]*' but was '$environmentVariable'")
        if (!name.matches(/[a-zA-Z][-_a-zA-Z0-9]*/))
            throw new GradleException("Name must match '[a-zA-Z][-_a-zA-Z0-9]*' but was '$name'")
        if (name in KEYWORDS)
            throw new GradleException("Name cannot be one of $KEYWORDS but was '$name'")
    }


    @Input
    String getDescription() {
        return this._description.getOrNull()
    }

    void setDescription(String description) {
        this._description.set(description)
    }

    void setDescription(Provider<String> description) {
        this._description.set(description)
    }

    void description(String description) {
        this._description.set(description)
    }


    @Input
    Map<String, String> getAliases() {
        return this._aliases.get()
    }

    void setAliases(Map<String, String> aliases) {
        this._aliases.set(aliases)
    }

    void setAliases(Provider<Map<String, String>> aliases) {
        this._aliases.set(aliases)
    }

    void aliases(Map<String, String> aliases) {
        this.aliases.putAll(aliases)
    }


    @Input
    String getDefaultValue() {
        return this._defaultValue.get()
    }

    void setDefaultValue(String defaultValue) {
        this._defaultValue.set(defaultValue)
    }

    void setDefaultValue(Provider<String> defaultValue) {
        this._defaultValue.set(defaultValue)
    }

    void defaultValue(String defaultValue) {
        this._defaultValue.set(defaultValue)
    }
}
