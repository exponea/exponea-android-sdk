package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

internal class AppInboxCacheImpl(
    context: Context,
    private val gson: Gson
) : AppInboxCache {
    companion object {
        internal const val FILENAME = "exponeasdk_app_inbox.json"
    }

    private class AppInboxData {
        var messages: List<MessageItem> = arrayListOf()
        var token: String? = null
    }

    private val storageFile = File(context.cacheDir, FILENAME)
    private var data: AppInboxData = ensureData()

    private fun ensureData(): AppInboxData {
        var result: AppInboxData
        try {
            if (storageFile.exists()) {
                val fileData = storageFile.readText()
                val type = object : TypeToken<AppInboxData>() {}.type
                val loadedData: AppInboxData = gson.fromJson(fileData, type)
                if (areValid(loadedData)) {
                    result = loadedData
                } else {
                    storageFile.delete()
                    result = AppInboxData()
                }
            } else {
                result = AppInboxData()
            }
        } catch (e: Throwable) {
            Logger.w(this, "Error getting stored AppInbox messages $e")
            result = AppInboxData()
        }
        return result
    }

    /**
     * Older SDK may store AppInboxData with MessageItem without required data.
     * We have to check and remove them in that case
     */
    private fun areValid(source: AppInboxData): Boolean {
        return source.messages.all { it.syncToken != null && it.customerIds.isNotEmpty() }
    }

    override fun setMessages(messages: List<MessageItem>) {
        data.messages = ArrayList(messages).sortedByDescending { it.receivedTime }
        storeData()
    }

    private fun storeData() {
        val file = createTempFile()
        file.writeText(gson.toJson(data))
        if (!file.renameTo(storageFile)) {
            Logger.e(this, "Renaming AppInbox file failed!")
        }
    }

    override fun clear(): Boolean {
        data.messages = arrayListOf()
        data.token = null
        return storageFile.delete()
    }

    override fun getMessages(): List<MessageItem> {
        return data.messages
    }

    override fun getSyncToken(): String? {
        return data.token
    }

    override fun setSyncToken(token: String?) {
        data.token = token
        storeData()
    }

    override fun addMessages(messages: List<MessageItem>) {
        val mapOfNew = messages.associateBy { it.id }
        val target = getMessages().associateBy { it.id }.toMutableMap()
        target.putAll(mapOfNew)
        setMessages(target.values.toList())
    }
}
