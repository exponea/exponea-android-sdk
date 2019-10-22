package com.exponea.sdk.models

internal data class CustomerExportAttributes(
        var type: String? = null,
        var list: MutableList<CustomerAttributes>? = null
)
