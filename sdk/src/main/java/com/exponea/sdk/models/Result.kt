package com.exponea.sdk.models

import com.google.gson.annotations.SerializedName

data class Result<V>(
    val success: Boolean?,

    @SerializedName("results", alternate = ["data", "messages", "in_app_content_blocks"])
    val results: V,

    @SerializedName("sync_token")
    val syncToken: String? = null

)
