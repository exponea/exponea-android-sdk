package com.exponea.sdk.telemetry.upload

import android.content.Context
import android.os.Build
import android.util.Base64
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.telemetry.TelemetryUtility
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.model.ErrorData
import com.exponea.sdk.telemetry.model.EventLog
import com.exponea.sdk.util.isReactNativeSDK
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
    private val uploadUrl: String = DEFAULT_UPLOAD_URL
) : TelemetryUpload {
    companion object {
        private const val DEFAULT_UPLOAD_URL = "https://in.appcenter.ms/logs?Api-Version=1.0.0"

        private val jsonMediaType: MediaType = MediaType.parse("application/json")!!
        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        init {
            isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private val APP_SECRET: String
        get() {
            return if (context.isReactNativeSDK()) {
                if (BuildConfig.DEBUG) {
                    "8308ba5f-319a-452e-99eb-826a0714e344"
                } else {
                    "0be0c184-73d2-49d2-aa90-31c3895c2c54"
                }
            } else {
                if (BuildConfig.DEBUG) {
                    "19dca50b-3467-488b-b1fa-47fb9258901a"
                } else {
                    "67e2bde9-3c20-4259-b8e4-428b4f89ca8d"
                }
            }
        }

    private val networkClient = OkHttpClient()

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

    override fun uploadSessionStart(runId: String, callback: (Result<Unit>) -> Unit) {
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
            ),
            callback
        )
    }

    fun upload(data: VSAppCenterAPIRequestData, callback: (Result<Unit>) -> Unit) {
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
                response.close()
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
        val appInfo = TelemetryUtility.getAppInfo(context)

        return VSAppCenterAPIDevice(
            appInfo.packageName,
            "${appInfo.packageName}-${appInfo.versionName}",
            appInfo.versionCode,
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
                    // There is a bug in VS App Center, it's reported.
                    // Logs with both .kt and .java extension fail to process
                    it.fileName.replace(".java", ".kt"),
                    it.lineNumber
                )
            },
            innerExceptions = if (errorData.cause != null)
                arrayListOf(getAPIException(errorData.cause)) else arrayListOf()
        )
    }
}
