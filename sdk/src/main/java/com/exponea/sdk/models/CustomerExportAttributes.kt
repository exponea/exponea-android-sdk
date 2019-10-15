package com.exponea.sdk.models

data class CustomerExportAttributes(
        var type: String? = null,
        var list: MutableList<CustomerAttributes>? = null
)
