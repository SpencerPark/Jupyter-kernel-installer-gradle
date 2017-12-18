package io.github.spencerpark.jupyter.gradle.installers

import spock.lang.Specification

class SimpleScriptGeneratorTest extends Specification {
    SimpleScriptGenerator generator = new SimpleScriptGenerator('    ', '\n')

    def 'simple replacement'() {
        setup:
        generator.putTokens token: 'replacement'
        def original = 'String with a @token@'
        def compiled = generator.compile(original)

        expect:
        compiled == 'String with a replacement'
    }

    def 'dynamic replacement closure'() {
        setup:
        generator['token'] = { it << 'test' }

        expect:
        generator.compile('@token@') == 'test'
    }

    def 'skips unknown'() {
        setup:
        def original = 'String with an @unknown_token@'
        def compiled = generator.compile(original)

        expect:
        compiled == 'String with an @unknown_token@'
    }

    def 'handles recursive tokens'() {
        setup:
        generator.putTokens token_top: 'nested @token_bot@', token_bot: 'token'
        def original = 'String with a @/r/token_top@ in it'
        def originalNoRecursion = 'String with a @token_top@ in it'
        def compiled = generator.compile(original)
        def compiledNoRecursion = generator.compile(originalNoRecursion)

        expect:
        compiled == 'String with a nested token in it'
        compiledNoRecursion == 'String with a nested @token_bot@ in it'
    }

    def 'builds a script'() {
        setup:
        generator.putTokens token: 'replacement'

        when:
        generator << 'Sample line with a @token@ in it\n'
        generator << 'A second line'

        then:
        generator.compiled == 'Sample line with a replacement in it\nA second line'
    }

    def 'indents properly'() {
        when:
        generator << 'Sample line\n'
        generator.indented {
            generator << 'Indented once\n'
            generator.indented {
                generator << 'Indented twice\n'
            }
            generator << 'Indented once'
        }

        then:
        generator.compiled == [
                'Sample line',
                '    Indented once',
                '        Indented twice',
                '    Indented once'
        ].join('\n')
    }

    def 'properly smart indents'() {
        setup:
        generator.putTokens token: 'multiline\nreplacement'

        when:
        generator << '  Two spaces before the @/s/token@'

        then:
        generator.compiled == [
                '  Two spaces before the multiline',
                '  replacement'
        ].join('\n')
    }

    def 'smart indent with indented token'() {
        setup:
        generator.putTokens token: { SimpleScriptGenerator out ->
            out << 'Simple line\n'
            out.indented {
                out << 'Indented line'
            }
        }

        when:
        generator << '    @/s/token@'

        then:
        generator.compiled == [
                '    Simple line',
                '        Indented line'
        ].join('\n')
    }

    def 'fixes indent'() {
        setup:
        generator << 'unindented\n  '
        generator << 'should be unindented'

        expect:
        generator.compiled == [
                'unindented',
                'should be unindented'
        ].join('\n')
    }
}
