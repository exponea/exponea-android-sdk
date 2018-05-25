package com.exponea.sdk.models

data class CustomerEvent(
        val type: String,
        val timestamp: Double,
        val properties: HashMap<String, String>
)