package com.exponea.sdk.models


data class FetchEventsRequest(
        var customerIds: CustomerIds = CustomerIds(),
        var eventTypes: MutableList<String>,
        var sortOrder: String = "desc",
        var limit: Int = 3,
        var skip: Int = 100
) {

        fun toHashMap() : HashMap<String, Any> {
                return hashMapOf(
                        "customer_ids" to customerIds.toHashMap(),
                        "event_types" to eventTypes,
                        "order" to sortOrder,
                        "limit" to limit,
                        "skip" to skip
                )
        }
}