package com.exponea.sdk.manager

import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.Route

interface EventManager {
    fun addEventToQueue(event: ExportedEventType, route: Route)
}