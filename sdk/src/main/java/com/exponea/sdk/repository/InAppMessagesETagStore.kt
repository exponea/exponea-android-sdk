package com.exponea.sdk.repository

import com.exponea.sdk.util.Logger
import java.util.concurrent.ConcurrentHashMap

internal interface InAppMessagesETagStore {
    fun store(etag: String, key: String)
    fun retrieve(key: String): String?
    fun remove(key: String)
    fun clearAll()
}

/**
 * Process-lifetime ETag store for in-app messages.
 * Values are intentionally kept only in memory and are never written to Android
 * persistent storage.
 */
internal class VolatileInAppMessagesETagStore : InAppMessagesETagStore {

    private val values = ConcurrentHashMap<String, String>()

    override fun store(etag: String, key: String) {
        if (key.isEmpty()) return

        values[key] = etag
        Logger.d(this, "[InApp] ETag stored in memory: key=${key.take(16)}... etag=${etag.take(16)}...")
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
