package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

internal class RecommendationsRequest(
    @SerializedName("customer_ids")
    val customerIds: Map<String, Any?>,
    @SerializedName("engine_id")
    val engineId: String,
    @SerializedName("fill_with_random")
    val fillWithRandom: Boolean,
    val size: Int = 10,
    val items: Map<String, String>? = null,
    @SerializedName("no_track")
    val noTrack: Boolean? = null,
    @SerializedName("catalog_attributes_whitelist")
    val catalogAttributesWhitelist: List<String>? = null
)
