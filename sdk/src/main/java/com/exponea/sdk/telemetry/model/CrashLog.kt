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
    val runId: String
) {
    constructor(e: Throwable, fatal: Boolean, launchDate: Date, runId: String) : this(
        id = UUID.randomUUID().toString(),
        errorData = TelemetryUtility.getErrorData(e),
        fatal = fatal,
        timestampMS = System.currentTimeMillis(),
        launchTimestampMS = launchDate.time,
        runId = runId
    )
}