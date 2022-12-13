package com.exponea.sdk.repository

import com.exponea.sdk.models.MessageItem

internal interface AppInboxCache {
    fun getMessages(): List<MessageItem>
    fun setMessages(messages: List<MessageItem>)
    fun clear(): Boolean
    fun getSyncToken(): String?
    fun setSyncToken(token: String?)
    fun addMessages(messages: List<MessageItem>)
}
