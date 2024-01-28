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

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

interface WithGradleDslExtensions {
    infix fun <V> Property<V>.by(value: V?) {
        this.set(value)
    }

    infix fun <V> Property<V>.by(provider: Provider<out V>) {
        this.set(provider)
    }

    infix fun <V> Property<V>.assign(value: V?) {
        this.set(value)
    }

    infix fun <V> Property<V>.assign(provider: Provider<out V>) {
        this.set(provider)
    }

    operator fun <K, V> MapProperty<K, V>.set(key: K, value: V) {
        this.put(key, value)
    }

    operator fun <K, V> MapProperty<K, V>.set(key: K, provider: Provider<out V>) {
        this.put(key, provider)
    }

    operator fun <V> ListProperty<V>.plusAssign(value: V) {
        this.add(value)
    }
}