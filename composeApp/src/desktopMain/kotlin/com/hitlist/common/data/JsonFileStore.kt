package com.hitlist.common.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Generic JSON-file persistence helper. It knows *how* to store any value on disk,
 * but nothing about *what* is stored, so it carries no dependency on any feature.
 * Each feature owns its own cache adapter on top of this.
 */
class JsonFileStore(private val cacheDir: File = defaultCacheDir()) {

    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    init {
        cacheDir.mkdirs()
    }

    fun file(name: String): File = File(cacheDir, name)

    inline fun <reified T> read(file: File): T? =
        runCatching {
            if (file.exists()) json.decodeFromString<T>(file.readText()) else null
        }.getOrNull()

    inline fun <reified T> write(file: File, value: T) {
        file.writeText(json.encodeToString(value))
    }

    companion object {
        fun defaultCacheDir() = File("cache")
    }
}
