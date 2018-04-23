package com.exponea.sdk.models

data class CustomerExportAttributes(
        var type: String,
        var list: MutableList<CustomerAttributes>
)