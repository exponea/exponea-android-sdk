package com.exponea.sdk.repository

import com.exponea.sdk.util.Logger
import java.util.concurrent.ConcurrentHashMap

internal interface InAppContentBlocksETagStore {
    fun store(key: String, etag: String)
    fun retrieve(key: String): String?
    fun remove(key: String)
    fun clearAll()
}

/**
 * Process-lifetime ETag store for in-app content blocks.
 * Values are intentionally kept only in memory and are never written to Android
 * persistent storage.
 */
internal class VolatileInAppContentBlocksETagStore : InAppContentBlocksETagStore {

    private val values = ConcurrentHashMap<String, String>()

    override fun store(key: String, etag: String) {
        if (key.isEmpty()) return

        values[key] = etag
        Logger.d(this, "InAppCB: ETag stored in memory: key=${key.take(16)}... etag=${etag.take(16)}...")
    }

    override fun retrieve(key: String): String? {
        if (key.isEmpty()) return null

        return values[key]
    }

    override fun remove(key: String) {
        if (key.isEmpty()) return

        values.remove(key)
    }

    override fun clearAll() {
        values.clear()
    }
}
