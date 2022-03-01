package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class DateFilter(
    val enabled: Boolean = false,
    @SerializedName("from_date")
    val fromDate: Int? = null,
    @SerializedName("to_date")
    val toDate: Int? = null
)
