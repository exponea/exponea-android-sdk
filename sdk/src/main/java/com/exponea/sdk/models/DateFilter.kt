package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

internal data class DateFilter(
        var enabled: Boolean = false,
        @SerializedName("from_date")
        var fromDate: Int? = null,
        @SerializedName("to_date")
        var toDate: Int? = null
)
