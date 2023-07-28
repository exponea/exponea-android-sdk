package com.exponea.sdk.services.inappcontentblock

import android.content.Context
import com.exponea.sdk.manager.EventManager
import com.exponea.sdk.manager.InAppContentBlockTrackingDelegate
import com.exponea.sdk.models.Constants.EventTypes
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.EventType.BANNER
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.util.GdprTracking

internal class InAppContentBlockTrackingDelegateImpl(
    context: Context,
    private val eventManager: EventManager
) : InAppContentBlockTrackingDelegate {
    private val deviceProperties = DeviceProperties(context)

    override fun track(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        action: String,
        interaction: Boolean,
        trackingAllowed: Boolean,
        text: String?,
        link: String?,
        error: String?
    ) {
        val properties = HashMap<String, Any>()
        properties.putAll(
            hashMapOf(
                "action" to action,
                "banner_id" to contentBlock.id,
                "banner_name" to contentBlock.name,
                "interaction" to interaction,
                "os" to "Android",
                "platform" to "Android",
                "type" to "in-app content block",
                "placeholder" to placeholderId
            ))
        properties.put("banner_type", contentBlock.contentType.name.lowercase())
        contentBlock.personalizedData?.variantId?.let {
            properties.put("variant_id", it)
        }
        contentBlock.personalizedData?.variantName?.let {
            properties.put("variant_name", it)
        }
        properties.putAll(deviceProperties.toHashMap())
        if (text != null) {
            properties["text"] = text
        }
        if (link != null) {
            properties["link"] = link
        }
        error?.let { properties["error"] = it }
        contentBlock.consentCategoryTracking?.let {
            properties["consent_category_tracking"] = it
        }
        if (GdprTracking.isTrackForced(link)) {
            properties["tracking_forced"] = true
        }
        eventManager.processTrack(
            eventType = EventTypes.banner,
            properties = properties,
            type = BANNER,
            trackingAllowed = trackingAllowed,
            customerIds = contentBlock.customerIds
        )
    }
}
