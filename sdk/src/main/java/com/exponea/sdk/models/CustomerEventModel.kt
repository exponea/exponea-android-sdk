package com.exponea.sdk.models

data class CustomerEventModel(
        val type: String,
        val timestamp: Double,
        val properties: HashMap<String, String>
)