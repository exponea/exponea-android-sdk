package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.models.InAppMessage
import com.google.gson.Gson

internal class InAppMessagesCacheImpl(
    context: Context,
    gson: Gson
) : SimpleDataCache<List<InAppMessage>>(context, gson, IN_APP_MESSAGES_FILENAME), InAppMessagesCache {
    companion object {
        internal const val IN_APP_MESSAGES_FILENAME = "exponeasdk_in_app_messages.json"
    }

    override fun set(messages: List<InAppMessage>) = setData(messages)

    override fun clear(): Boolean = clearData()

    override fun get(): List<InAppMessage> = getData() ?: emptyList()

    override fun getTimestamp(): Long = getDataLastModifiedMillis()
}
