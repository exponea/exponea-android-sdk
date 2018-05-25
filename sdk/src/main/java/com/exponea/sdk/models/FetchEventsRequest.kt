package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class FetchEventsRequest(
        @SerializedName("customer_ids")
        var customerIds: CustomerIds = CustomerIds(),
        var eventTypes: MutableList<String>,
        var sortOrder: String = "desc",
        var limit: Int = 3,
        var skip: Int = 100
)