package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

internal data class Banner(
        @SerializedName("customer_ids")
        var customerIds: CustomerIds = CustomerIds(),
        @SerializedName("personalisation_ids")
        var personalizationIds: MutableList<String>? = mutableListOf(),
        var timeout: Int = 2,
        var timezone: String = "Europe/Bratislava",
        var params: HashMap<String, String> = hashMapOf()
)