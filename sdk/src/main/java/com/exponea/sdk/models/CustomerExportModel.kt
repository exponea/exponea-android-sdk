package com.exponea.sdk.models

internal data class CustomerExportModel(
    var attributes: CustomerExportAttributes,
    var filter: HashMap<String, String>,
    var executionTime: Int,
    var timezone: String,
    var responseFormat: String
)
