package com.exponea.sdk.telemetry.upload

import android.content.Context
import android.os.Build
import android.util.Base64
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.model.ErrorData
import com.exponea.sdk.telemetry.model.EventLog
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

internal class VSAppCenterTelemetryUpload(
    private val context: Context,
    private val installId: String,
    private val sdkVersion: String,
    private val userId: String,
    runId: String,
    private val uploadUrl: String = DEFAULT_UPLOAD_URL
) : TelemetryUpload {
    companion object {
        private const val DEFAULT_UPLOAD_URL = "https://in.appcenter.ms/logs?Api-Version=1.0.0"
        private const val APP_SECRET = "67e2bde9-3c20-4259-b8e4-428b4f89ca8d"

        private val jsonMediaType: MediaType = MediaType.parse("application/json")!!
        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        init {
            isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private val networkClient = OkHttpClient()

    init {
        uploadSessionStart(runId)
    }

    override fun uploadEventLog(log: EventLog, callback: (Result<Unit>) -> Unit) {
        upload(VSAppCenterAPIRequestData(logs = arrayListOf(getAPIEventLog(log))), callback)
    }

    override fun uploadCrashLog(log: CrashLog, callback: (Result<Unit>) -> Unit) {
        var logs: ArrayList<VSAppCenterAPILog> = arrayListOf(getAPIErrorLog(log))
        if (log.logs != null) {
            logs.add(getAPIErrorAttachment(log))
        }
        upload(VSAppCenterAPIRequestData(logs), callback)
    }

    private fun uploadSessionStart(runId: String) {
        upload(
            VSAppCenterAPIRequestData(
                logs = arrayListOf(
                    VSAppCenterAPIStartSession(
                        id = UUID.randomUUID().toString(),
                        timestamp = isoDateFormat.format(Date()),
                        sid = runId,
                        device = getAPIDevice()
                    )
                )
            )
        ) {
            Logger.i(this, "Session start ${if (it.isSuccess) "succeeded" else "failed" }")
        }
    }

    private fun upload(data: VSAppCenterAPIRequestData, callback: (Result<Unit>) -> Unit) {
        val requestData = Gson().toJson(data)
        val requestBuilder = Request.Builder()
            .url(uploadUrl)
            .addHeader("App-Secret", APP_SECRET)
            .addHeader("Install-ID", installId)
            .post(RequestBody.create(jsonMediaType, requestData))

        networkClient.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                // even if server rejects the log we call it a success - it was processed
                callback(Result.success(Unit))
            }
        })
    }

    private fun getAPIEventLog(log: EventLog): VSAppCenterAPIEventLog {
        return VSAppCenterAPIEventLog(
            id = log.id,
            sid = log.runId,
            userId = userId,
            device = getAPIDevice(),
            timestamp = isoDateFormat.format(Date(log.timestampMS)),
            name = log.name,
            properties = log.properties
        )
    }

    private fun getAPIErrorLog(log: CrashLog): VSAppCenterAPIErrorLog {
        return VSAppCenterAPIErrorLog(
            id = log.id,
            sid = log.runId,
            fatal = log.fatal,
            userId = userId,
            device = getAPIDevice(),
            exception = getAPIException(log.errorData),
            timestamp = isoDateFormat.format(Date(log.timestampMS)),
            appLaunchTimestamp = isoDateFormat.format(Date(log.launchTimestampMS))
        )
    }

    private fun getAPIErrorAttachment(log: CrashLog): VSAppCenterAPIErrorAttachmentLog {
        val logString = log.logs?.joinToString("\n") ?: ""
        val data = Base64.encodeToString(logString.toByteArray(), Base64.NO_WRAP)
        return VSAppCenterAPIErrorAttachmentLog(
            id = UUID.randomUUID().toString(),
            sid = log.runId,
            userId = userId,
            device = getAPIDevice(),
            timestamp = isoDateFormat.format(Date(log.timestampMS)),
            errorId = log.id,
            contentType = "text/plain",
            data = data
        )
    }

    private fun getAPIDevice(): VSAppCenterAPIDevice {
        var appNamespace = "unknown package"
        var appVersion = "unknown version"
        var appBuild = "unknown build"
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            appNamespace = packageInfo.packageName
            appVersion = packageInfo.versionName
            @Suppress("DEPRECATION")
            appBuild = packageInfo.versionCode.toString()
        } catch (e: Exception) {
            // do nothing
        }

        return VSAppCenterAPIDevice(
            appNamespace,
            appVersion,
            appBuild,
            sdkName = "ExponeaSDK.android",
            sdkVersion = sdkVersion,
            osName = "Android",
            osVersion = Build.VERSION.RELEASE,
            model = Build.MODEL,
            locale = Locale.getDefault().toString()
        )
    }

    private fun getAPIException(errorData: ErrorData): VSAppCenterAPIException {
        return VSAppCenterAPIException(
            type = errorData.type,
            message = errorData.message,
            frames = errorData.stackTrace.map {
                VSAppCenterAPIExceptionFrame(
                    it.className,
                    it.methodName,
                    it.fileName.replace(".java", ".kt"), // There is a bug in VS App Center, it's reported. Logs with both .kt and .java extension fail to process
                    it.lineNumber
                )
            },
            innerExceptions = if (errorData.cause != null)
                arrayListOf(getAPIException(errorData.cause)) else arrayListOf()
        )
    }
}