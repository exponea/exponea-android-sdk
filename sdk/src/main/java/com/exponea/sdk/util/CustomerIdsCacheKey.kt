package com.exponea.sdk.util

internal fun buildCustomerIdsCacheKey(customerIds: Map<String, String?>): String {
    return customerIds
        .toList()
        .sortedBy { it.first }
        .joinToString("&") { (key, value) -> "$key=${value ?: ""}" }
}
