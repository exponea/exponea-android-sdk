package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.view.InAppContentBlockPlaceholderView

interface InAppContentBlockManager {
    fun getPlaceholderView(
        placeholderId: String,
        context: Context,
        config: InAppContentBlockPlaceholderConfiguration
    ): InAppContentBlockPlaceholderView
    fun loadInAppContentBlockPlaceholders()
    fun clearAll()
    fun onEventCreated(event: Event, type: EventType)
}

internal interface InAppContentBlockTrackingDelegate {
    fun track(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        action: String,
        interaction: Boolean,
        trackingAllowed: Boolean,
        text: String? = null,
        link: String? = null,
        error: String? = null
    )
}
