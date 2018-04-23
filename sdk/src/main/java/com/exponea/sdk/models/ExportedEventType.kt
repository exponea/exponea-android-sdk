package com.exponea.sdk.models

data class ExportedEventType(var type: String?,
                             var timestamp: Double?,
                             var properties: Array<HashMap<String, String>>?,
                             var errors: Array<HashMap<String, String>>?)