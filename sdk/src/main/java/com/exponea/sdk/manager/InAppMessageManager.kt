package com.exponea.sdk.manager

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
        timestamp: Double?,
        trackingDelegate: InAppMessageTrackingDelegate
    ): Job?

    fun sessionStarted(sessionStartDate: Date)
    fun preloadIfNeeded(timestamp: Double)
    fun trackClickEvent(
        message: InAppMessage,
        trackingDelegate: InAppMessageTrackingDelegate,
        buttonText: String?,
        buttonLink: String?
    )
    fun trackCloseEvent(
        message: InAppMessage,
        trackingDelegate: InAppMessageTrackingDelegate
    )
    fun trackErrorEvent(
        message: InAppMessage,
        error: String,
        trackingDelegate: InAppMessageTrackingDelegate
    )
}

internal interface InAppMessageTrackingDelegate {
    fun track(
        message: InAppMessage,
        action: String,
        interaction: Boolean,
        text: String? = null,
        link: String? = null,
        error: String? = null
    )
}
