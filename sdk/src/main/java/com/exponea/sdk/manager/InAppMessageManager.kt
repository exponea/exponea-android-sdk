package com.exponea.sdk.manager

import com.exponea.sdk.models.InAppMessage
import kotlinx.coroutines.Job

internal interface InAppMessageManager {
    fun preload(callback: ((Result<Unit>) -> Unit)? = null)
    fun getRandom(eventType: String): InAppMessage?
    fun showRandom(eventType: String, trackingDelegate: InAppMessageTrackingDelegate): Job
}

internal interface InAppMessageTrackingDelegate {
    fun track(message: InAppMessage, action: String, interaction: Boolean)
}
