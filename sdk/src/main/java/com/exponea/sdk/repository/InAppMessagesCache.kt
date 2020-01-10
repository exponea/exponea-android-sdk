package com.exponea.sdk.repository

import com.exponea.sdk.models.InAppMessage

internal interface InAppMessagesCache {
    fun get(): List<InAppMessage>
    fun set(messages: List<InAppMessage>)
    fun clear(): Boolean
}
