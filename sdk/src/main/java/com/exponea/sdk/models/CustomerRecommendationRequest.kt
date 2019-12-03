package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

internal class CustomerRecommendationRequest internal constructor(
    customerIds: Map<String, Any?>,
    options: CustomerRecommendationOptions
) : CustomerAttributesRequest(customerIds = customerIds, attributes = arrayListOf(options))

data class CustomerRecommendationOptions(
    val id: String,
    val fillWithRandom: Boolean,
    val size: Int = 10,
    val items: Map<String, String>? = null,
    @SerializedName("no_track")
    val noTrack: Boolean? = null,
    val catalogAttributesWhitelist: List<String>? = null
) : CustomerAttributes {
    override val type = "recommendation"
}
