package com.exponea.sdk.manager

import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentationDataCallback
import com.exponea.sdk.services.OnIntegrationStoppedCallback

internal interface SegmentsManager : OnIntegrationStoppedCallback {
    fun onEventUploaded(event: ExportedEvent)
    fun onCallbackAdded(callback: SegmentationDataCallback)
    fun reload()
    fun clearAll()
    fun onSdkInit()
    fun fetchSegmentsManually(category: String, forceFetch: Boolean, callback: (List<Segment>) -> Unit)
    override fun onIntegrationStopped()
}
