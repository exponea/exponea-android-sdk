package com.exponea.sdk.telemetry

import android.content.Context
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.telemetry.model.EventLog
import com.exponea.sdk.telemetry.storage.FileTelemetryStorage
import com.exponea.sdk.telemetry.storage.TelemetryStorage
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.telemetry.upload.VSAppCenterTelemetryUpload
import com.exponea.sdk.util.Logger
import java.util.Date
import java.util.UUID

internal class TelemetryManager(context: Context, userId: String? = null) {
    companion object {
        const val TELEMETRY_PREFS_KEY = "EXPONEA_TELEMETRY"
        const val INSTALL_ID_KEY = "INSTALL_ID"
    }

    private var installId: String
    init {
        try {
            val prefs = context.getSharedPreferences(TELEMETRY_PREFS_KEY, 0)
            installId = prefs.getString(INSTALL_ID_KEY, null) ?: UUID.randomUUID().toString()
            if (!prefs.contains(INSTALL_ID_KEY)) {
                prefs.edit().putString(INSTALL_ID_KEY, installId).commit()
            }
        } catch (e: Exception) {
            installId = "placeholder-install-id"
        }
    }

    private val telemetryStorage: TelemetryStorage = FileTelemetryStorage(context)
    private val telemetryUpload: TelemetryUpload = VSAppCenterTelemetryUpload(
        context,
        installId,
        BuildConfig.VERSION_NAME,
        userId ?: installId
    )

    private val crashManager: CrashManager = CrashManager(telemetryStorage, telemetryUpload, Date())

    fun start() {
        crashManager.start()
    }

    fun reportEvent(name: String, properties: Map<String, String>) {
        telemetryUpload.uploadEventLog(EventLog(name, properties)) {
            Logger.i(this, "Event upload ${if (it.isSuccess) "succeeded" else "failed" }")
        }
    }
}