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

    override fun getMessages(): List<MessageItem> = getData()?.messages ?: emptyList()

    override fun getSyncToken(): String? = getData()?.token

    override fun getApplicationId(): String? = getData()?.applicationId

    @Synchronized
    override fun setMessages(messages: List<MessageItem>) {
        val data = getData() ?: AppInboxData()
        data.messages = ArrayList(messages).sortedByDescending { it.receivedTime }
        setData(data)
    }

    @Synchronized
    override fun setSyncToken(token: String?) {
        val data = getData() ?: AppInboxData()
        data.token = token
        setData(data)
    }

    @Synchronized
    private fun setApplicationId(applicationId: String) {
        val data = getData() ?: AppInboxData()
        data.applicationId = applicationId
        setData(data)
    }

    @Synchronized
    override fun addMessages(messages: List<MessageItem>) {
        val mapOfNew = messages.associateBy { it.id }
        val target = getMessages().associateBy { it.id }.toMutableMap()
        target.putAll(mapOfNew)
        setMessages(target.values.toList())
    }

    override fun clear(): Boolean = clearData()

    @Synchronized
    override fun clearAndSetApplicationId() {
        clear()
        setApplicationId(applicationId)
    }
}
