package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.SegmentationData
import com.google.gson.Gson

internal class SegmentsCacheImpl(
    context: Context,
    gson: Gson
) : SimpleDataCache<SegmentationData>(context, gson, SEGMENTS_FILENAME), SegmentsCache {
    companion object {
        internal const val SEGMENTS_FILENAME = "exponeasdk_segments.json"
        internal const val CACHE_AGE_MILLIS = 5000L
    }

    override fun get(): SegmentationData? = getData()

    override fun set(segments: SegmentationData) = setData(segments)

    override fun clear(): Boolean = clearData()

    override fun isFresh(): Boolean {
        val cachedAtMillis = get()?.updatedAtMillis ?: return false
        return cachedAtMillis >= System.currentTimeMillis() - CACHE_AGE_MILLIS
    }

    override fun isAssignedTo(customerIds: CustomerIds): Boolean {
        return get()?.customerIds == customerIds
    }
}
