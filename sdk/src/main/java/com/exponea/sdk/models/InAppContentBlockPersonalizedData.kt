package com.exponea.sdk.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.Date

data class InAppContentBlockPersonalizedData(
    @SerializedName("id")
    val blockId: String,
    @SerializedName("status")
    var rawStatus: String?,
    @SerializedName("ttl_seconds")
    var timeToLive: Int?,
    @SerializedName("has_tracking_consent")
    var rawHasTrackingConsent: Boolean?,
    @SerializedName("variant_id")
    var variantId: Int?,
    @SerializedName("variant_name")
    var variantName: String?,
    @SerializedName("content_type")
    var rawContentType: String?,
    @SerializedName("content")
    var content: Map<String, Any?>?
) {
    @Expose
    var loadedAt: Date? = null

    val status: InAppContentBlockStatus get() = InAppContentBlockStatus.parseValue(rawStatus)

    val hasTrackingConsent: Boolean get() = rawHasTrackingConsent ?: true
}
