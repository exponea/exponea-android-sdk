package com.exponea.sdk.models

data class Result<V>(
        val success: Boolean,
        val results: V
)