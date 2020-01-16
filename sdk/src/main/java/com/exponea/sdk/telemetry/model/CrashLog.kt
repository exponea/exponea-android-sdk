package com.exponea.sdk.telemetry.model

import com.exponea.sdk.telemetry.TelemetryUtility
import java.util.Date
import java.util.UUID

internal data class CrashLog(
    val id: String,
    val errorData: ErrorData,
    val fatal: Boolean,
    val timestampMS: Long,
    val launchTimestampMS: Long,
    val runId: String,
    val logs: List<String>? = null
) {
    constructor(
        e: Throwable,
        fatal: Boolean,
        date: Date,
        launchDate: Date,
        runId: String,
        logs: List<String>? = null
    ) : this(
        id = UUID.randomUUID().toString(),
        errorData = TelemetryUtility.getErrorData(e),
        fatal = fatal,
        timestampMS = date.time,
        launchTimestampMS = launchDate.time,
        runId = runId,
        logs = logs
    )
}
