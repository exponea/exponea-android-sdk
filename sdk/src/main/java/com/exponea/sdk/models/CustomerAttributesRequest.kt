package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

abstract class CustomerAttributesRequest(
    @SerializedName("customer_ids")
    val customerIds: Map<String, Any?>,
    val attributes: List<CustomerAttributes>
)

interface CustomerAttributes {
    val type: String
}
