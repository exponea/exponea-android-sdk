package com.exponea.sdk.telemetry.model

import com.exponea.sdk.telemetry.TelemetryUtility
import java.util.Date
import java.util.UUID

internal data class CrashLog(
    val id: String,
    val errorData: ErrorData,
    val timestampMS: Long,
    val launchTimestampMS: Long,
    val runId: String
) {
    constructor(e: Throwable, launchDate: Date, runId: String) : this(
        id = UUID.randomUUID().toString(),
        errorData = TelemetryUtility.getErrorData(e),
        timestampMS = System.currentTimeMillis(),
        launchTimestampMS = launchDate.time,
        runId = runId
    )
}