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
        handleException(e, true)
        oldHandler?.uncaughtException(t, e)
    }

    fun handleException(e: Throwable, fatal: Boolean) {
        try {
            val crashLog = CrashLog(e, fatal, launchDate, runId)
            if (fatal) { // app is crashing, save exception, process it later
                if (TelemetryUtility.isSDKRelated(e)) {
                    Logger.i(this, "Fatal exception is sdk related, saving for later upload.")
                    storage.saveCrashLog(crashLog)
                }
            } else { // we should have time to immediately upload the exception
                upload.uploadCrashLog(crashLog) { result ->
                    Logger.i(this, "Crash log upload ${if (result.isSuccess) "succeeded" else "failed"}")
                }
            }
        } catch (e: Exception) {
            // do nothing
        }
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