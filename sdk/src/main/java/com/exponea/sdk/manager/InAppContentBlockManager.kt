package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppContentBlockPlaceholderConfiguration
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.services.inappcontentblock.InAppContentBlockDataLoader
import com.exponea.sdk.view.InAppContentBlockPlaceholderView

interface InAppContentBlockManager : OnIntegrationStoppedCallback {
    fun getPlaceholderView(
        placeholderId: String,
        context: Context,
        config: InAppContentBlockPlaceholderConfiguration
    ): InAppContentBlockPlaceholderView
    fun getPlaceholderView(
        placeholderId: String,
        dataLoader: InAppContentBlockDataLoader,
        context: Context,
        config: InAppContentBlockPlaceholderConfiguration
    ): InAppContentBlockPlaceholderView
    fun loadInAppContentBlockPlaceholders(inAppContentBlockPlaceholdersAutoLoad: List<String> = emptyList())
    /** Internal asynchronous bridge used by the runtime ICB controller. */
    fun loadPlaceholderAsync(
        placeholderId: String,
        forceRefresh: Boolean = false,
        completion: (Boolean) -> Unit
    )
    /** Clears data derived from the supplied placeholders without affecting other placeholders. */
    fun invalidatePlaceholders(placeholderIds: List<String>)
    /** True only when a currently eligible, renderable block is available locally. */
    fun hasRenderableContent(placeholderId: String): Boolean
    fun clearAll()
    fun onEventCreated(event: Event, type: EventType)
    fun getAllInAppContentBlocksForPlaceholder(placeholderId: String): List<InAppContentBlock>
    fun passesFilters(contentBlock: InAppContentBlock): Boolean
    fun loadContentIfNeededSync(contentBlocks: List<InAppContentBlock>, forceRefresh: Boolean = false)
    fun passesFrequencyFilter(contentBlock: InAppContentBlock): Boolean
    fun passesDateFilter(contentBlock: InAppContentBlock): Boolean
    override fun onIntegrationStopped()
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
