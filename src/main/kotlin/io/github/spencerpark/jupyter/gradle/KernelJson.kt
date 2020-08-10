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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonObject
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.Input
import java.io.File

class KernelJson(
        @Input val installedKernelJar: String,
        @Input val kernelDisplayName: String,
        @Input val kernelLanguage: String,
        @Input val interruptMode: String,
        @Input val kernelEnvironment: Map<String, String>
) {
    private val compiledSpec: String = Json(JsonConfiguration.Stable.copy(prettyPrint = true)).stringify(
            JsonElement.serializer(),
            JsonObject(mapOf(
                    "argv" to JsonArray(listOf("java", "-jar", installedKernelJar, "{connection_file}").map(::JsonLiteral)),
                    "display_name" to JsonLiteral(kernelDisplayName),
                    "language" to JsonLiteral(kernelLanguage),
                    "interrupt_mode" to JsonLiteral(interruptMode),
                    "env" to JsonObject(kernelEnvironment.mapValues { JsonLiteral(it.value) })
            ))
    )

    constructor(
            installedKernelJar: RegularFile,
            kernelDisplayName: String,
            kernelLanguage: String,
            interruptMode: String,
            kernelEnvironment: Map<String, String>
    ) : this(
            installedKernelJar.asFile.absolutePath.toString().replace(File.separatorChar, '/'),
            kernelDisplayName,
            kernelLanguage,
            interruptMode,
            kernelEnvironment
    )

    override fun toString() = compiledSpec
}
