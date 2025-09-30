package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.util.Logger
import com.google.gson.Gson

internal class AppInboxCacheImpl(
    context: Context,
    gson: Gson,
    private val applicationId: String
) : SimpleDataCache<AppInboxCacheImpl.AppInboxData>(context, gson, FILENAME), AppInboxCache {
    companion object {
        internal const val FILENAME = "exponeasdk_app_inbox.json"
    }

    init {
        clearCacheIfAppIdHasChanged()
    }

    private fun clearCacheIfAppIdHasChanged() {
        if (applicationId != getApplicationId()) {
            clearAndSetApplicationId()
            Logger.v(this, "AppInboxCache: application Id has changed -> clear cache.")
        }
    }

    internal class AppInboxData {
        var messages: List<MessageItem> = arrayListOf()
        var token: String? = null
        var applicationId: String? = null
    }

    private fun ensureData(): AppInboxData {
        var data = getData()
        if (data == null) {
            synchronized(this) {
                data = getData()
                if (data == null) {
                    val emptyData = AppInboxData()
                    setData(emptyData)
                    data = emptyData
                }
            }
        }
        return data ?: AppInboxData()
    }

    override fun setMessages(messages: List<MessageItem>) {
        val data = ensureData()
        data.messages = ArrayList(messages).sortedByDescending { it.receivedTime }
        setData(data)
    }

    override fun getMessages(): List<MessageItem> {
        return ensureData().messages
    }

    override fun getSyncToken(): String? {
        return ensureData().token
    }

    override fun setSyncToken(token: String?) {
        val data = ensureData()
        data.token = token
        setData(data)
    }

    private fun setApplicationId(applicationId: String) {
        val data = ensureData()
        data.applicationId = applicationId
        setData(data)
    }

    override fun getApplicationId(): String? {
        return ensureData().applicationId
    }

    override fun addMessages(messages: List<MessageItem>) {
        val mapOfNew = messages.associateBy { it.id }
        val target = getMessages().associateBy { it.id }.toMutableMap()
        target.putAll(mapOfNew)
        setMessages(target.values.toList())
    }

    override fun clear(): Boolean = clearData()
    override fun clearAndSetApplicationId() {
        clear()
        setApplicationId(applicationId)
    }
}
