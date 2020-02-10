package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

internal class InAppMessagesCacheImpl(
    context: Context,
    private val gson: Gson
) : InAppMessagesCache {
    companion object {
        internal const val IN_APP_MESSAGES_FILENAME = "exponeasdk_in_app_messages.json"
    }

    private val storageFile = File(context.cacheDir, IN_APP_MESSAGES_FILENAME)
    private var data: List<InAppMessage>? = null

    override fun set(messages: List<InAppMessage>) {
        val file = createTempFile()
        file.writeText(gson.toJson(messages))
        clear()
        if (!file.renameTo(storageFile)) {
            Logger.e(this, "Renaming in-app message file failed!")
        }
    }

    override fun clear(): Boolean {
        data = null
        return storageFile.delete()
    }

    override fun get(): List<InAppMessage> {
        data?.let { return it }
        try {
            if (storageFile.exists()) {
                val fileData = storageFile.readText()
                val type = object : TypeToken<List<InAppMessage>>() {}.type
                data = gson.fromJson(fileData, type)
                data?.let { return it }
            }
        } catch (e: Throwable) {
            Logger.w(this, "Error getting stored in app messages $e")
        }
        return arrayListOf()
    }

    override fun getTimestamp(): Long {
        return storageFile.lastModified()
    }
}
