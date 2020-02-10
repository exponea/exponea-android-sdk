package com.exponea.sdk.manager

import com.exponea.sdk.models.InAppMessage
import java.util.Date
import kotlinx.coroutines.Job

internal interface InAppMessageManager {
    fun preload(callback: ((Result<Unit>) -> Unit)? = null)

    fun getRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?
    ): InAppMessage?

    fun showRandom(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?,
        trackingDelegate: InAppMessageTrackingDelegate
    ): Job

    fun sessionStarted(sessionStartDate: Date)
}

internal interface InAppMessageTrackingDelegate {
    fun track(message: InAppMessage, action: String, interaction: Boolean)
}
