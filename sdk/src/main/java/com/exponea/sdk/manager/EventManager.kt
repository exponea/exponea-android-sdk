package com.exponea.sdk.manager

import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExportedEventType

internal interface EventManager {
    fun addEventToQueue(event: ExportedEventType, eventType: EventType)
}
