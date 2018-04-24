package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class ExponeaFetchId (
        @SerializedName("customer_ids")
        var customerIds: HashMap<String, String> = hashMapOf(),
        var id: String? = null
)