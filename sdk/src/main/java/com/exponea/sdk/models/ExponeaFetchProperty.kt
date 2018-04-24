package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class ExponeaFetchProperty(
        @SerializedName("customer_ids")
        var customerIds: HashMap<String, String> = hashMapOf(),
        var property: String? = null
)