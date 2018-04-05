package io.github.spencerpark.jupyter.gradle.installers

import groovy.transform.CompileStatic
import org.gradle.api.Nullable

@CompileStatic
class InstallerParameterSpec {
    public static final String PATH_SEPARATOR = '\0\1'
    public static final String FILE_SEPARATOR = '\0\2'

    enum Type { STRING, FLOAT }

    String name
    String envVar
    @Nullable String description
    @Nullable String defaultValue
    private Type _type = Type.STRING
    @Nullable String listSep
    @Nullable List<String> choices
    @Nullable Map<String, String> aliases

    InstallerParameterSpec(String name, String envVar) {
        this.name = name
        this.envVar = envVar
    }

    Type getType() {
        return this._type
    }

    void setType(Type type) {
        if (type == null)
            type = Type.STRING
        this._type = type
    }

    void usePathSeparator() {
        this.listSep = PATH_SEPARATOR
    }

    void useFileSeparator() {
        this.listSep = FILE_SEPARATOR
    }

    void addChoice(String choice) {
        if (this.choices == null)
            this.choices = []
        this.choices.add(choice)
    }

    void addAlias(String name, String replacement) {
        if (this.aliases == null)
            this.aliases = [:]
        this.aliases.put(name, replacement)
    }
}
