package com.exponea.sdk.manager

import com.exponea.sdk.models.ExportedEvent
import com.exponea.sdk.models.Segment
import com.exponea.sdk.models.SegmentationDataCallback

internal interface SegmentsManager {
    fun onEventUploaded(event: ExportedEvent)
    fun onCallbackAdded(callback: SegmentationDataCallback)
    fun reload()
    fun clearAll()
    fun onSdkInit()
    fun fetchSegmentsManually(category: String, forceFetch: Boolean, callback: (List<Segment>) -> Unit)
}
