package com.exponea.sdk.manager

import com.exponea.sdk.models.EventType
import com.exponea.sdk.util.currentTimeSeconds

internal interface EventManager {
    fun track(
        eventType: String? = null,
        timestamp: Double? = currentTimeSeconds(),
        properties: HashMap<String, Any> = hashMapOf(),
        type: EventType,
        customerIds: Map<String, String?>? = null
    )
    fun processTrack(
        eventType: String? = null,
        timestamp: Double? = currentTimeSeconds(),
        properties: HashMap<String, Any> = hashMapOf(),
        type: EventType,
        trackingAllowed: Boolean,
        customerIds: Map<String, String?>? = null
    )
}
