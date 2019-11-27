package com.exponea.sdk.telemetry.storage

import com.exponea.sdk.telemetry.model.CrashLog

internal interface TelemetryStorage {
    fun saveCrashLog(log: CrashLog)
    fun deleteCrashLog(log: CrashLog)
    fun getAllCrashLogs(): List<CrashLog>
}
