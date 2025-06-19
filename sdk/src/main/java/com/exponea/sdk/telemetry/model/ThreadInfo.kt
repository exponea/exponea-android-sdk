package com.exponea.sdk.telemetry.model

internal data class ThreadInfo(
    val id: Long,
    val name: String,
    val state: String,
    val isDaemon: Boolean,
    val isCurrent: Boolean,
    val isMain: Boolean,
    val stackTrace: List<ErrorStackTraceElement>
)
