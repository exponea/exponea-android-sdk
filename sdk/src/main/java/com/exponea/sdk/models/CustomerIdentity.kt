package com.exponea.sdk.models

/**
 * Represents customer identity information including customer IDs and optional authentication token.
 *
 * @param customerIds Map of customer identifier keys to their values.
 * @param sdkAuthToken Optional authentication token for the customer.
 */
class CustomerIdentity @JvmOverloads constructor(
    val customerIds: Map<String, String?>,
    val sdkAuthToken: String? = null
)
