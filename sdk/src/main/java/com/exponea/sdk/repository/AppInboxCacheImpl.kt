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
    private var data: AppInboxData? = null

    override fun setMessages(messages: List<MessageItem>) {
        ensureData().messages = ArrayList(messages).sortedByDescending { it.receivedTime }
        storeData()
    }

    private fun ensureData(): AppInboxData {
        if (data == null) {
            synchronized(this) {
                if (data == null) {
                    try {
                        if (storageFile.exists()) {
                            val fileData = storageFile.readText()
                            val type = object : TypeToken<AppInboxData>() {}.type
                            data = gson.fromJson(fileData, type)
                        } else {
                            data = AppInboxData()
                        }
                    } catch (e: Throwable) {
                        Logger.w(this, "Error getting stored AppInbox messages $e")
                        data = AppInboxData()
                    }
                }
            }
        }
        return data!!
    }

    private fun storeData() {
        val file = createTempFile()
        file.writeText(gson.toJson(data))
        if (!file.renameTo(storageFile)) {
            Logger.e(this, "Renaming AppInbox file failed!")
        }
    }

    override fun clear(): Boolean {
        data?.let {
            it.messages = arrayListOf()
            it.token = null
        }
        data = null
        return storageFile.delete()
    }

    override fun getMessages(): List<MessageItem> {
        return ensureData().messages
    }

    override fun getSyncToken(): String? {
        return ensureData().token
    }

    override fun setSyncToken(token: String?) {
        ensureData().token = token
        storeData()
    }

    override fun addMessages(messages: List<MessageItem>) {
        val mapOfNew = messages.associateBy { it.id }
        val target = getMessages().associateBy { it.id }.toMutableMap()
        target.putAll(mapOfNew)
        setMessages(target.values.toList())
    }
}
