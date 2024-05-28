package com.exponea.sdk.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.Date

data class InAppContentBlockPersonalizedData(
    @SerializedName("id")
    val blockId: String,
    @SerializedName("status")
    val rawStatus: String?,
    @SerializedName("ttl_seconds")
    val timeToLive: Int?,
    @SerializedName("has_tracking_consent")
    val rawHasTrackingConsent: Boolean?,
    @SerializedName("variant_id")
    val variantId: Int?,
    @SerializedName("variant_name")
    val variantName: String?,
    @SerializedName("content_type")
    val rawContentType: String?,
    @SerializedName("content")
    val content: Map<String, Any?>?
) {
    @Expose
    var loadedAt: Date? = null

    val status: InAppContentBlockStatus get() = InAppContentBlockStatus.parseValue(rawStatus)

    val hasTrackingConsent: Boolean get() = rawHasTrackingConsent ?: true
}
