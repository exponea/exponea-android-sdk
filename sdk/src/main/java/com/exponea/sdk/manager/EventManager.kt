package com.exponea.sdk.manager

import com.exponea.sdk.models.EventType
import com.exponea.sdk.util.currentTimeSeconds

internal interface EventManager {
    fun track(
        eventType: String? = null,
        timestamp: Double? = currentTimeSeconds(),
        properties: HashMap<String, Any> = hashMapOf(),
        type: EventType
    )
}
