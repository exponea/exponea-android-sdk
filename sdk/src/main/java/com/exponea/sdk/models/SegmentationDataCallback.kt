package com.exponea.sdk.models

import com.exponea.sdk.Exponea

abstract class SegmentationDataCallback {
    abstract val exposingCategory: String
    abstract val includeFirstLoad: Boolean
    abstract fun onNewData(segments: List<Segment>)
    final fun unregister() {
        Exponea.unregisterSegmentationDataCallback(this)
    }
}
