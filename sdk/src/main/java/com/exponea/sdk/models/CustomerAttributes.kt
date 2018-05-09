package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class CustomerAttributes(
        @SerializedName("customer_ids")
        var customerId: CustomerIds = CustomerIds(),
        @SerializedName("attributes")
        var attributes: MutableList<HashMap<String, String>>? = null
)