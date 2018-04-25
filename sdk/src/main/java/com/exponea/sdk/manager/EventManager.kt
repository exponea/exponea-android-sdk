package com.exponea.sdk.manager

import com.exponea.sdk.models.ExportedEventType

interface EventManager {
    fun addEventToQueue(event: ExportedEventType)
}