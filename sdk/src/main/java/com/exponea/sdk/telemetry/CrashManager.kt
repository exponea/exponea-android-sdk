package com.exponea.sdk.telemetry

import com.exponea.sdk.Exponea
import com.exponea.sdk.services.OnIntegrationStoppedCallback
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.storage.TelemetryStorage
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.util.Logger
import java.util.Date
import java.util.LinkedList

internal class CrashManager(
    private val storage: TelemetryStorage,
    private val upload: TelemetryUpload,
    private val launchDate: Date,
    private val runId: String
) : Thread.UncaughtExceptionHandler, OnIntegrationStoppedCallback {
    companion object {
        private const val SDK_PACKAGE = "com.exponea"
        const val MAX_LOG_MESSAGES = 100
        const val LOG_RETENTION_MS = 1000 * 60 * 60 * 24 * 15 // 15 days
    }
    private var oldHandler: Thread.UncaughtExceptionHandler? = null
    private val logMessagesLock = Any()
    internal var latestLogMessages: LinkedList<String> = LinkedList()

    fun start() {
        if (Exponea.isStopped) {
            Logger.e(this, "Crash manager not started, SDK is stopping")
            return
        }
        Logger.i(this, "Starting crash manager")
        uploadCrashLogs()
        oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (Exponea.isStopped) {
            Logger.e(this, "Crash has not been handled, SDK is stopping")
        } else {
            Logger.i(this, "Handling uncaught exception(app crash)")
            handleException(e, true, t)
        }
        oldHandler?.uncaughtException(t, e)
    }

    fun handleException(e: Throwable, fatal: Boolean, t: Thread) {
        try {
            val crashLog = CrashLog(
                e,
                fatal,
                Date(),
                launchDate,
                runId,
                getLatestLogMessagesSnapshot(),
                t
            )
            if (fatal) { // app is crashing, save exception, process it later
                if (isSDKRelated(e)) {
                    Logger.i(this, "Fatal exception is sdk related, saving for later upload.")
                    storage.saveCrashLog(crashLog)
                }
            } else { // we should have time to immediately upload the exception
                upload.uploadCrashLog(crashLog) { result ->
                    if (result.isSuccess) {
                        Logger.i(this, "Crash log upload succeeded.")
                    } else {
                        Logger.i(this, "Crash log upload failed, will retry later.")
                        storage.saveCrashLog(crashLog)
                    }
                }
            }
        } catch (_: Exception) {
            // do nothing
        }
    }

    private fun isSDKRelated(e: Throwable): Boolean {
        var current: Throwable? = e
        val visited = mutableSetOf<Throwable>()
        while (current != null && !visited.contains(current)) {
            if (current.stackTrace.any {
                    it.className.startsWith(SDK_PACKAGE) &&
                            it.className != CrashManager::class.java.name
                }) {
                return true
            }
            visited.add(current)
            current = current.cause
        }
        return false
    }

    private fun getLatestLogMessagesSnapshot(): List<String> = synchronized(logMessagesLock) {
        latestLogMessages.toList()
    }

    fun saveLogMessage(parent: Any, message: String, timestamp: Long) = synchronized(logMessagesLock) {
        latestLogMessages.add(0, "${Date(timestamp)} ${parent.javaClass.simpleName}: $message")
        while (latestLogMessages.size > MAX_LOG_MESSAGES) {
            latestLogMessages.removeAt(latestLogMessages.size - 1)
        }
    }

    private fun uploadCrashLogs() {
        try {
            storage.getAllCrashLogs().forEach { crashLog ->
                if (System.currentTimeMillis() - crashLog.timestampMS > LOG_RETENTION_MS) {
                    storage.deleteCrashLog(crashLog)
                    return@forEach
                }
                Logger.i(this, "Uploading crash log ${crashLog.id}")
                upload.uploadCrashLog(crashLog) { result ->
                    Logger.i(this, "Crash log upload ${if (result.isSuccess) "succeeded" else "failed" }")
                    if (result.isSuccess) {
                        storage.deleteCrashLog(crashLog)
                    }
                }
            }
        } catch (_: Exception) {
            // do nothing
        }
    }

    override fun onIntegrationStopped() {
        synchronized(logMessagesLock) {
            latestLogMessages.clear()
        }
        val activeHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (activeHandler != this) {
            // current CrashManager instance is not the active handler,
            // therefore `oldHandler` could be obsolete
            return
        }
        Thread.setDefaultUncaughtExceptionHandler(oldHandler)
    }
}
