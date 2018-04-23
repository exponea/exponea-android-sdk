package com.exponea.sdk.models

data class ExportedEventType(
        var type: String?,
        var timestamp: Double?,
        var properties: HashMap<String, String>?,
        var errors: HashMap<String, String>?
)