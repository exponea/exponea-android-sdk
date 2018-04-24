package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class CustomerRecommendation(
        @SerializedName("customer_ids")
        var customerIds: HashMap<String, String>? = hashMapOf()
        var type: String? = null,
        var id: String? = null,
        var size: Int? = null,
        var strategy: String? = null,
        var knowItems: Boolean? = null,
        var anti: Boolean? = null,
        var items: HashMap<String, String>? = hashMapOf()
)