package com.exponea.sdk.models

data class FetchError(
    val jsonBody: String?,
    val message: String,
    val httpCode: Int? = null
)
