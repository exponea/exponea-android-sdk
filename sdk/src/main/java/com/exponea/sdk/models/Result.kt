package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class Result<V>(
        val success: Boolean,

        @SerializedName("results", alternate = ["data"])
        val results: V

)