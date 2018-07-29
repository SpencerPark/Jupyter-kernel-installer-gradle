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

import spock.lang.Specification

class PythonScriptGeneratorTest extends Specification {
    PythonScriptGenerator generator = new PythonScriptGenerator()

    InstallerParameterSpec strParam, floatParam, aliasParam, choiceParam, listParam, fileListParam, descParam, defaultParam

    void setup() {
        strParam = new InstallerParameterSpec('param-1', 'ENV_1')

        floatParam = new InstallerParameterSpec('param-2', 'ENV_2')
        floatParam.type = InstallerParameterSpec.Type.FLOAT

        aliasParam = new InstallerParameterSpec('param-3', 'ENV_3')
        aliasParam.type = InstallerParameterSpec.Type.FLOAT
        aliasParam.addAlias('ON', '1')
        aliasParam.addAlias('OFF', '0')

        choiceParam = new InstallerParameterSpec('param-4', 'ENV_4')
        choiceParam.choices = ['a', 'b', 'c']

        listParam = new InstallerParameterSpec('param-5', 'ENV_5')
        listParam.listSep = ' '

        fileListParam = new InstallerParameterSpec('param-6', 'ENV_6')
        fileListParam.useFileSeparator()

        descParam = new InstallerParameterSpec('param-7', 'ENV_7')
        descParam.description = '''\
            A "multiline"
            'description'
        '''.stripIndent()

        defaultParam = new InstallerParameterSpec('param-8', 'ENV_8')
        defaultParam.defaultValue = 'DEFAULT'
    }

    void addAllParams() {
        generator.addParameter(strParam)
        generator.addParameter(floatParam)
        generator.addParameter(aliasParam)
        generator.addParameter(choiceParam)
        generator.addParameter(listParam)
        generator.addParameter(fileListParam)
        generator.addParameter(descParam)
        generator.addParameter(defaultParam)
    }

    def 'properly generates an alias dictionary body'() {
        setup:
        addAllParams()

        when:
        generator << '@ARG_ALIASES_DICT@'

        then:
        generator.compiled == [
                '"ENV_3": {',
                '    "ON": "1",',
                '    "OFF": "0",',
                '},',
                '',
        ].join('\n')
    }

    def 'properly generates a name map body'() {
        setup:
        addAllParams()

        when:
        String compiled = generator.compile('    @/s/NAME_MAP_DICT@')

        then:
        compiled == [
                '    "param-1": "ENV_1",',
                '    "param-2": "ENV_2",',
                '    "param-3": "ENV_3",',
                '    "param-4": "ENV_4",',
                '    "param-5": "ENV_5",',
                '    "param-6": "ENV_6",',
                '    "param-7": "ENV_7",',
                '    "param-8": "ENV_8",',
                '    ',
        ].join('\n')
    }

    def 'properly generates default value replacements'() {
        setup:
        addAllParams()

        when:
        generator << '@GENERATED_DEFAULT_REPLACEMENT@'

        then:
        generator.compiled == [
                'if not hasattr(args, "env") or getattr(args, "env") is None:',
                '    setattr(args, "env", {})',
                'getattr(args, "env").setdefault("ENV_8", "DEFAULT")',
                '',
        ].join('\n')
    }

    private static String argumentCode(Map<String, String> opts, String name) {
        def lines = [
                'parser.add_argument(',
                "    \"--$name\",",
                '    dest="env",',
                '    action=EnvVar,',
                '    aliases=ALIASES,',
                '    name_map=NAME_MAP,',
        ]

        opts.each { key, value ->
            lines.add("    $key=$value,")
        }

        lines.add(')')
        lines.add('')

        return lines.join('\n')
    }

    def 'properly generates argument parser for str'() {
        setup:
        generator.addParameter(strParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == argumentCode(strParam.name, type: 'type_assertion("param-1", str)')
    }

    def 'properly generates argument parser for float'() {
        setup:
        generator.addParameter(floatParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == argumentCode(floatParam.name, type: 'type_assertion("param-2", float)')
    }

    def 'properly generates argument parser for alias'() {
        setup:
        generator.addParameter(aliasParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == argumentCode(aliasParam.name, type: 'type_assertion("param-3", float)')
    }

    def 'properly generates argument parser for choice'() {
        setup:
        generator.addParameter(choiceParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == argumentCode(choiceParam.name, type: 'type_assertion("param-4", str)', choices: '["a", "b", "c", ]')
    }

    def 'properly generates argument parser for list'() {
        setup:
        generator.addParameter(listParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == argumentCode(listParam.name, type: 'type_assertion("param-5", str)', list_sep: '" "')
    }

    def 'properly generates argument parser for file list'() {
        setup:
        generator.addParameter(fileListParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == argumentCode(fileListParam.name, type: 'type_assertion("param-6", str)', list_sep: 'os.sep')
    }

    def 'properly generates argument parser for desc'() {
        setup:
        generator.addParameter(descParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == argumentCode(descParam.name, help: '"A \\"multiline\\"\\n\'description\'\\n"', type: 'type_assertion("param-7", str)', )
    }

    def 'properly generates argument parser for default'() {
        setup:
        generator.addParameter(defaultParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == argumentCode(defaultParam.name, type: 'type_assertion("param-8", str)')
    }
}
