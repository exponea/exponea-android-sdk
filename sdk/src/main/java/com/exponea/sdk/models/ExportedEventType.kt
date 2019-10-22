package com.exponea.sdk.models

import com.exponea.sdk.util.currentTimeSeconds
import com.google.gson.annotations.SerializedName

internal data class ExportedEventType(
        @SerializedName("project_id")
        var projectId: String? = null,
        @SerializedName("event_type")
        var type: String? = null,
        var timestamp: Double? = currentTimeSeconds(),
        @SerializedName("customer_ids")
        var customerIds: HashMap<String, Any?>? = null,
        @SerializedName("properties")
        var properties: HashMap<String, Any>? = null,
        var errors: HashMap<String, String>? = null
)
