package com.exponea.sdk.models

data class DateFilter (
        var enabled: Boolean = false,
        var fromDate: Int? = null,
        var toDate: Int? = null
)