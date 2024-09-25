package com.exponea.sdk.repository

import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.SegmentationData

internal interface SegmentsCache {
    fun get(): SegmentationData?
    fun set(segments: SegmentationData)
    fun clear(): Boolean
    fun isFresh(): Boolean
    fun isAssignedTo(customerIds: CustomerIds): Boolean
}
