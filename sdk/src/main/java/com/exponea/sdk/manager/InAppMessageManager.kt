package com.exponea.sdk.manager

import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.InAppMessage
import java.util.Date
import kotlinx.coroutines.Job

internal interface InAppMessageManager {
    fun preload(callback: ((Result<Unit>) -> Unit)? = null)

    fun getFilteredMessages(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?,
        requireImageLoaded: Boolean = true
    ): List<InAppMessage>

    fun getRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?,
        requireImageLoaded: Boolean = true
    ): InAppMessage?

    fun showRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?
    ): Job?

    fun sessionStarted(sessionStartDate: Date)
    fun preloadIfNeeded(timestamp: Double)
    fun onEventCreated(event: Event, type: EventType)
}

internal interface InAppMessageTrackingDelegate {
    fun track(
        message: InAppMessage,
        action: String,
        interaction: Boolean,
        trackingAllowed: Boolean,
        text: String? = null,
        link: String? = null,
        error: String? = null
    )
}
