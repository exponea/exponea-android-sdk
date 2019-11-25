package com.exponea.sdk.telemetry

import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.storage.TelemetryStorage
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.util.Logger
import java.util.Date

internal class CrashManager(
    private val storage: TelemetryStorage,
    private val upload: TelemetryUpload,
    private val launchDate: Date,
    private val runId: String
) : Thread.UncaughtExceptionHandler {
    private var oldHandler: Thread.UncaughtExceptionHandler? = null

    fun start() {
        Logger.i(this, "Starting crash manager")
        uploadCrashLogs()
        oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Logger.i(this, "Handling uncaught exception")
        try {
            if (TelemetryUtility.isSDKRelated(e)) {
                Logger.i(this, "Uncaught exception is sdk related, saving for later upload.")
                storage.saveCrashLog(CrashLog(e, launchDate, runId))
            }
        } catch (e: Exception) {
            // do nothing
        }

        oldHandler?.uncaughtException(t, e)
    }

    private fun uploadCrashLogs() {
        try {
            storage.getAllCrashLogs().map { crashLog ->
                Logger.i(this, "Uploading crash log ${crashLog.id}")
                upload.uploadCrashLog(crashLog) { result ->
                    Logger.i(this, "Crash log upload ${if (result.isSuccess) "succeeded" else "failed" }")
                    if (result.isSuccess) {
                        storage.deleteCrashLog(crashLog)
                    }
                }
            }
        } catch (e: Exception) {
            // do nothing
        }
    }
}