package com.exponea.sdk.telemetry.upload

import com.exponea.sdk.telemetry.model.CrashLog

internal interface TelemetryUpload {
    fun uploadCrashLog(log: CrashLog, callback: (Result<Unit>) -> Unit)
}