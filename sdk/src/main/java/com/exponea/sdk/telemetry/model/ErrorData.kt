package com.exponea.sdk.telemetry.model

internal data class ErrorData(
    val type: String,
    val message: String,
    val stackTrace: List<ErrorStackTraceElement>,
    val cause: ErrorData?
)