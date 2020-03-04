package com.exponea.sdk.models

import com.exponea.sdk.util.currentTimeSeconds
import com.google.gson.annotations.SerializedName

internal data class ExportedEventType(
    @SerializedName("event_type")
    var type: String? = null,
    var timestamp: Double? = currentTimeSeconds(),
    @SerializedName("customer_ids")
    var customerIds: HashMap<String, String?>? = null,
    @SerializedName("properties")
    var properties: HashMap<String, Any>? = null
)
