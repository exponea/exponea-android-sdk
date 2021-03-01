package com.exponea.sdk.telemetry

import android.app.Application
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.telemetry.model.EventLog
import com.exponea.sdk.telemetry.model.EventType
import com.exponea.sdk.telemetry.storage.FileTelemetryStorage
import com.exponea.sdk.telemetry.storage.TelemetryStorage
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.telemetry.upload.VSAppCenterTelemetryUpload
import com.exponea.sdk.util.Logger
import java.util.Date
import java.util.UUID

internal class TelemetryManager(application: Application, userId: String? = null) {
    companion object {
        const val TELEMETRY_PREFS_KEY = "EXPONEA_TELEMETRY"
        const val INSTALL_ID_KEY = "INSTALL_ID"
    }

    private val appInfo = TelemetryUtility.getAppInfo(application)
    private val runId = UUID.randomUUID().toString()
    private var installId: String
    init {
        try {
            val prefs = application.getSharedPreferences(TELEMETRY_PREFS_KEY, 0)
            installId = prefs.getString(INSTALL_ID_KEY, null) ?: UUID.randomUUID().toString()
            if (!prefs.contains(INSTALL_ID_KEY)) {
                prefs.edit().putString(INSTALL_ID_KEY, installId).commit()
            }
        } catch (e: Exception) {
            installId = "placeholder-install-id"
        }
    }

    private val telemetryStorage: TelemetryStorage = FileTelemetryStorage(application)
    private val telemetryUpload: TelemetryUpload = VSAppCenterTelemetryUpload(
        application,
        installId,
        BuildConfig.EXPONEA_VERSION_NAME,
        userId ?: installId
    )

    private val crashManager: CrashManager = CrashManager(telemetryStorage, telemetryUpload, Date(), runId)

    fun start() {
        crashManager.start()
        telemetryUpload.uploadSessionStart(runId) {
            Logger.i(this, "Session start upload ${if (it.isSuccess) "succeeded" else "failed" }")
        }
    }

    fun reportEvent(eventType: EventType, properties: MutableMap<String, String> = hashMapOf()) {
        val mutableProperties = properties.toMutableMap()
        mutableProperties.putAll(hashMapOf(
            "sdkVersion" to BuildConfig.EXPONEA_VERSION_NAME,
            "appName" to appInfo.packageName,
            "appVersion" to appInfo.versionName,
            "appNameVersion" to "${appInfo.packageName} - ${appInfo.versionName}",
            "appNameVersionSdkVersion"
                to "${appInfo.packageName} - ${appInfo.versionName} - SDK ${BuildConfig.EXPONEA_VERSION_NAME}"
        ))
        telemetryUpload.uploadEventLog(EventLog(eventType.value, mutableProperties, runId)) {
            Logger.i(this, "Event upload ${if (it.isSuccess) "succeeded" else "failed" }")
        }
    }

    fun reportInitEvent(configuration: ExponeaConfiguration) {
        reportEvent(EventType.INIT, TelemetryUtility.formatConfigurationForTracking(configuration))
    }

    fun reportCaughtException(e: Throwable) {
        crashManager.handleException(e, false)
    }

    fun reportLog(parent: Any, message: String, timestamp: Long? = null) {
        crashManager.saveLogMessage(parent, message, timestamp ?: System.currentTimeMillis())
    }
}
