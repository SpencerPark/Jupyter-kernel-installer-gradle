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
}
