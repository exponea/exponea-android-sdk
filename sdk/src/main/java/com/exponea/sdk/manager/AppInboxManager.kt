package com.exponea.sdk.manager

import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.MessageItem

interface AppInboxManager {
    fun fetchAppInbox(callback: (List<MessageItem>?) -> Unit)
    fun fetchAppInboxItem(messageId: String, callback: (MessageItem?) -> Unit)
    fun reload()
    fun onEventCreated(event: Event, type: EventType)
    fun markMessageAsRead(messageId: MessageItem, callback: ((Boolean) -> Unit)?)
}
