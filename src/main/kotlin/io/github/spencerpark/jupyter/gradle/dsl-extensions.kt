package io.github.spencerpark.jupyter.gradle

import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

interface WithGradleDslExtensions {
    operator fun <K, V> MapProperty<K, V>.set(key: K, value: V) {
        this.put(key, value)
    }

    operator fun <K, V> MapProperty<K, V>.set(key: K, provider: Provider<out V>) {
        this.put(key, provider)
    }

    infix fun <V> Property<V>.by(value: V?) {
        this.set(value)
    }

    infix fun <V> Property<V>.by(provider: Provider<out V>) {
        this.set(provider)
    }
}