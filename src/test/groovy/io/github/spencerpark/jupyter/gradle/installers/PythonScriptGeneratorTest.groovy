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
            A multiline
            description
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
                "'ENV_3': {",
                "    'ON': '1',",
                "    'OFF': '0',",
                "}",
                "",
        ].join('\n')
    }

    def 'properly generates a name map body'() {
        setup:
        addAllParams()

        when:
        String compiled = generator.compile('    @/s/NAME_MAP_DICT@')

        then:
        compiled == [
                "    'param-1': 'ENV_1',",
                "    'param-2': 'ENV_2',",
                "    'param-3': 'ENV_3',",
                "    'param-4': 'ENV_4',",
                "    'param-5': 'ENV_5',",
                "    'param-6': 'ENV_6',",
                "    'param-7': 'ENV_7',",
                "    'param-8': 'ENV_8',",
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
                "if not hasattr(args, 'env') or getattr(args, 'env') is None:",
                "    setattr(args, 'env', {})",
                "getattr(args, 'env').setdefault('ENV_8', 'DEFAULT')",
                "",
        ].join('\n')
    }

    private String argumentCode( Map<String, String> opts, String name) {
        def lines = [
                'parser.add_argument(',
                "    '--$name',",
                "    dest='env',",
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
        generator.compiled == this.argumentCode(strParam.name, type: 'str')
    }

    def 'properly generates argument parser for float'() {
        setup:
        generator.addParameter(floatParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == this.argumentCode(floatParam.name, type: 'float')
    }

    def 'properly generates argument parser for alias'() {
        setup:
        generator.addParameter(aliasParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == this.argumentCode(aliasParam.name, type: 'float')
    }

    def 'properly generates argument parser for choice'() {
        setup:
        generator.addParameter(choiceParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == this.argumentCode(choiceParam.name, type: 'str', choices: "['a', 'b', 'c', ]")
    }

    def 'properly generates argument parser for list'() {
        setup:
        generator.addParameter(listParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == this.argumentCode(listParam.name, type: 'str', list_sep: "' '")
    }

    def 'properly generates argument parser for file list'() {
        setup:
        generator.addParameter(fileListParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == this.argumentCode(fileListParam.name, type: 'str', list_sep: 'os.sep')
    }

    def 'properly generates argument parser for desc'() {
        setup:
        generator.addParameter(descParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == this.argumentCode(descParam.name, help: 'json.loads(\'"A multiline\\ndescription\\n"\')', type: 'str', )
    }

    def 'properly generates argument parser for default'() {
        setup:
        generator.addParameter(defaultParam)

        when:
        generator << '@GENERATED_ARGS@'

        then:
        generator.compiled == this.argumentCode(defaultParam.name, type: 'str')
    }
}
