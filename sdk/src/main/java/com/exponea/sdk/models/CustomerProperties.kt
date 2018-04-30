package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class CustomerProperties (
        @SerializedName("customer_ids")
        var customerIds: CustomerIds = CustomerIds(),
        @SerializedName("properties")
        var properties: HashMap<String, Any>
)