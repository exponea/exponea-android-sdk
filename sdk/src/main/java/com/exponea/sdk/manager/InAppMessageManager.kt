package com.exponea.sdk.manager

import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import java.util.Date

internal interface InAppMessageManager : OnIntegrationStoppedCallback {
    fun reload(
        callback: ((Result<Unit>) -> Unit)? = null
    )

    fun findMessagesByFilter(
        eventType: String,
        properties: Map<String, Any?>,
        timestamp: Double?
    ): List<InAppMessage>

    fun sessionStarted(sessionStartDate: Date)
    fun onEventCreated(event: Event, type: EventType)
    fun onEventUploaded(event: ExportedEvent)
    fun clear()
    override fun onIntegrationStopped()
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
