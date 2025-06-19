package com.exponea.sdk.telemetry.model

internal data class ErrorStackTraceElement(
    val className: String?,
    val methodName: String?,
    val fileName: String?,
    val lineNumber: Int
)
