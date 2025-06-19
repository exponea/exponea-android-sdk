package com.exponea.sdk.telemetry.upload

import android.app.Application
import android.net.Uri
import android.os.Build
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.telemetry.TelemetryUtility
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.model.ErrorData
import com.exponea.sdk.telemetry.model.ErrorStackTraceElement
import com.exponea.sdk.telemetry.model.EventLog
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.isCalledFromExampleApp
import com.exponea.sdk.util.isFlutterSDK
import com.exponea.sdk.util.isMauiSDK
import com.exponea.sdk.util.isReactNativeSDK
import com.exponea.sdk.util.isXamarinSDK
import com.exponea.sdk.util.logOnException
import com.google.gson.GsonBuilder
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

internal class SentryTelemetryUpload(
    application: Application,
    private val installId: String
) : TelemetryUpload {

    companion object {
        private const val STACKTRACE_FRAMES_LIMIT = 100
        private val dateFormatter = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }
        }
        private val gson = GsonBuilder().disableHtmlEscaping().create()
    }

    private val appInfo = TelemetryUtility.getAppInfo(application)

    private val isRunningTest: Boolean =
        try {
            Class.forName("com.exponea.ExponeaTestClass")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

    private val dsn = if (application.isCalledFromExampleApp() || isRunningTest) {
        // Use dev app center project when SDK is used in our demo app
        "https://0c1ab20fe28a048ab96370522875d4f6@msdk.bloomreach.co/10"
    } else {
        when {
            application.isReactNativeSDK() -> {
                "https://374c34109a5a1002c79a66b70b7b49ae@msdk.bloomreach.co/4"
            }
            application.isFlutterSDK() -> {
                "https://9a1334c7487d1bce28415518b9d98644@msdk.bloomreach.co/6"
            }
            application.isXamarinSDK() -> {
                "https://e979cf922de68c65f817b1fc22148db6@msdk.bloomreach.co/11"
            }
            application.isMauiSDK() -> {
                "https://a2c4b65ebd4b2603abc74c6d0c2590d1@msdk.bloomreach.co/8"
            }
            // Android native SDK
            else -> "https://78099a909addd590ed19c21826405b73@msdk.bloomreach.co/2"
        }
    }

    private val sentryUserinfo = Uri.parse(dsn).userInfo
    private val sentryHostname = Uri.parse(dsn).host
    private val sentryProject = Uri.parse(dsn).lastPathSegment
    internal var sentryEnvelopeApiUrl = "https://$sentryHostname/api/$sentryProject/envelope/"

    override fun uploadSessionStart(runId: String, callback: (Result<Unit>) -> Unit) = runCatching {
        val dateFormat = dateFormatter.get()
        if (dateFormat == null) {
            Logger.e(this, "Unable to report session to sentry, missing datetime formatter")
            return@runCatching
        }
        val now = Date()
        val sentAtString = dateFormat.format(now)
        val sequence: Long = now.time.let {
            // if device has wrong date and time and it is nearly at the beginning of the epoch time.
            // when converting GMT to UTC may give a negative value.
            if (it < 0) {
                return@let abs(it.toDouble()).toLong()
            } else {
                return@let it
            }
        }
        val envelopeHeader = ExponeaSentryEnvelopeHeader(
            eventId = null,
            dsn = dsn,
            sentAt = sentAtString
        )
        val itemBody = ExponeaSentrySession(
            started = sentAtString,
            timestamp = sentAtString,
            distinctId = runId,
            sessionId = runId,
            init = true,
            sequence = sequence,
            attributes = ExponeaSentryAttributes(
                release = buildSentryReleaseInfo(),
                environment = BuildConfig.BUILD_TYPE
            ),
            extra = buildTags("sentrySession")
        )
        val envelopeItem = ExponeaSentryEnvelopeItem(itemBody.prepareHeader(), itemBody)
        val sentryEnvelope = ExponeaSentryEnvelope(envelopeHeader, envelopeItem)
        sendSentryEnvelope(sentryEnvelope, callback)
    }.logOnException()

    override fun uploadCrashLog(log: CrashLog, callback: (Result<Unit>) -> Unit) = runCatching {
        val dateFormat = dateFormatter.get()
        if (dateFormat == null) {
            Logger.e(this, "Unable to report crash to sentry, missing datetime formatter")
            return@runCatching
        }
        val sentryEventId = log.id.replace("-", "").lowercase()
        val sentAtString = dateFormat.format(Date(log.timestampMS))
        val envelopeHeader = ExponeaSentryEnvelopeHeader(
            eventId = sentryEventId,
            dsn = dsn,
            sentAt = sentAtString
        )
        val itemBody = ExponeaSentryException(
            timestamp = sentAtString,
            threads = ExponeaSentryValuesWrapper(extractThreads(log)),
            exception = ExponeaSentryValuesWrapper(extractExceptionsQueue(log)),
            level = toSentryErrorLevel(log.fatal),
            eventId = sentryEventId,
            contexts = buildContexts(),
            tags = buildTags("sentryError"),
            release = buildSentryReleaseInfo(),
            environment = BuildConfig.BUILD_TYPE,
            extra = emptyMap()
        )
        val envelopeItem = ExponeaSentryEnvelopeItem(itemBody.prepareHeader(), itemBody)
        val sentryEnvelope = ExponeaSentryEnvelope(envelopeHeader, envelopeItem)
        sendSentryEnvelope(sentryEnvelope, callback)
    }.logOnException()

    private fun extractExceptionsQueue(log: CrashLog): List<ExponeaSentryExceptionPart> {
        val circularityDetector = hashSetOf<ErrorData>()
        val exceptions = mutableListOf<ExponeaSentryExceptionPart>()
        extractExceptionsQueueInternally(
            log.errorData,
            circularityDetector,
            exceptions,
            log.fatal,
            log.thread.id
        )
        // clean resources for GC
        circularityDetector.clear()
        // Sentry impl adds to `exceptions` in reverse order according to:
        // `Multiple values represent chained exceptions and should be sorted oldest to newest.`
        exceptions.reverse()
        return exceptions
    }

    private fun extractExceptionsQueueInternally(
        throwable: ErrorData?,
        circularityDetector: HashSet<ErrorData>,
        exceptions: MutableList<ExponeaSentryExceptionPart>,
        fatal: Boolean,
        threadId: Long
    ) {
        var currentThrowable: ErrorData? = throwable
        while (currentThrowable != null && circularityDetector.add(currentThrowable)) {
            val stackTraceFrames = parseExceptionStackTrace(currentThrowable.stackTrace)
            exceptions.add(ExponeaSentryExceptionPart(
                type = currentThrowable.type,
                value = currentThrowable.message,
                stacktrace = ExponeaSentryStackTrace(
                    frames = stackTraceFrames
                ),
                mechanism = ExponeaSentryExceptionMechanism(
                    type = "generic",
                    description = "generic",
                    handled = !fatal
                ),
                threadId = threadId
            ))
            val suppressed = currentThrowable.suppressed ?: emptyList()
            suppressed.forEach {
                extractExceptionsQueueInternally(
                    it, circularityDetector, exceptions, fatal, threadId
                )
            }
            currentThrowable = currentThrowable.cause
        }
    }

    private fun parseExceptionStackTrace(stackTrace: List<ErrorStackTraceElement>): List<ExponeaSentryStackFrame> {
        return stackTrace.map { each ->
            ExponeaSentryStackFrame(
                filename = each.fileName ?: "",
                function = each.methodName,
                module = each.className,
                // Protocol doesn't accept negative line numbers.
                // The runtime seem to use -2 as a way to signal a native method
                lineno = max(each.lineNumber, 0)
            )
        }
        // Sentry impl does limit frames
        .take(STACKTRACE_FRAMES_LIMIT)
        // and then it reverses the order
        .reversed()
        .toList()
    }

    private fun extractThreads(log: CrashLog): List<ExponeaSentryThread> {
        if (log.thread == null) {
            return emptyList()
        }
        val thread = log.thread
        return listOf(ExponeaSentryThread(
            id = thread.id,
            name = thread.name,
            state = thread.state,
            crashed = log.fatal,
            current = thread.isCurrent,
            daemon = thread.isDaemon,
            main = thread.isMain,
            stacktrace = ExponeaSentryStackTrace(
                frames = parseExceptionStackTrace(thread.stackTrace)
            )
        ))
    }

    private fun toSentryErrorLevel(fatal: Boolean): String {
        return if (fatal) {
            "fatal"
        } else {
            "error"
        }
    }

    override fun uploadEventLog(log: EventLog, callback: (Result<Unit>) -> Unit) = runCatching {
        val dateFormat = dateFormatter.get()
        if (dateFormat == null) {
            Logger.e(this, "Unable to report event to sentry, missing datetime formatter")
            return@runCatching
        }
        val sentryEventId = log.id.replace("-", "").lowercase()
        val sentAtString = dateFormat.format(Date(log.timestampMS))
        val envelopeHeader = ExponeaSentryEnvelopeHeader(
            eventId = sentryEventId,
            dsn = dsn,
            sentAt = sentAtString
        )
        val itemBody = ExponeaSentryMessage(
            timestamp = sentAtString,
            message = ExponeaSentryMessageLog(log.name),
            eventId = sentryEventId,
            contexts = buildContexts(),
            tags = buildTags(log.name),
            release = buildSentryReleaseInfo(),
            environment = BuildConfig.BUILD_TYPE,
            extra = log.properties
        )
        val envelopeItem = ExponeaSentryEnvelopeItem(itemBody.prepareHeader(), itemBody)
        val sentryEnvelope = ExponeaSentryEnvelope(envelopeHeader, envelopeItem)
        sendSentryEnvelope(sentryEnvelope, callback)
    }.logOnException()

    internal fun sendSentryEnvelope(
        sentryEnvelope: ExponeaSentryEnvelope,
        callback: (Result<Unit>) -> Unit
    ) {
        val request = buildRequest(sentryEnvelope)
        val networkClient = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }).build()
        networkClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                callback(Result.success(Unit))
                response.close()
            }
        })
    }

    private fun buildRequest(sentryEnvelope: ExponeaSentryEnvelope): Request {
        val messageHeaderJson = gson.toJson(sentryEnvelope.header)
        val messageItemJson = gson.toJson(sentryEnvelope.item.body)
        val messageItemHeaderJson = gson.toJson(sentryEnvelope.item.header.withLength(messageItemJson.length))
        val multilineJsonContent = """
                $messageHeaderJson
                $messageItemHeaderJson
                $messageItemJson
            """.trimIndent()
        val request = Request.Builder()
            .url(sentryEnvelopeApiUrl)
            .addHeader("Content-Type", "application/x-sentry-envelope")
            .addHeader("User-Agent", "sentry.java.android/8.3.0")
            .addHeader(
                "X-Sentry-Auth",
                "Sentry sentry_version=7,sentry_client=sentry.java.android/8.3.0,sentry_key=$sentryUserinfo"
            )
            .post(RequestBody.create("application/json".toMediaTypeOrNull()!!, multilineJsonContent))
            .build()
        return request
    }

    private fun buildContexts(): ExponeaSentryContext {
        return ExponeaSentryContext(
            app = ExponeaSentryAppContextInfo(
                appIdentifier = appInfo.packageName,
                appName = appInfo.appName,
                appBuild = appInfo.versionCode
            ),
            os = ExponeaSentryOsContextInfo(
                version = Build.VERSION.RELEASE
            ),
            device = ExponeaSentryDeviceContextInfo(
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER,
                brand = Build.BRAND
            )
        )
    }

    private fun buildTags(eventName: String): Map<String, String> {
        return mapOf(
            "uuid" to installId,
            "projectToken" to tryReadProjectToken(),
            "sdkVersion" to BuildConfig.EXPONEA_VERSION_NAME,
            "sdkName" to "ExponeaSDK.android",
            "appName" to appInfo.appName,
            "appVersion" to appInfo.versionName,
            "appBuild" to appInfo.versionCode,
            "appIdentifier" to appInfo.packageName,
            "osName" to "Android",
            "osVersion" to Build.VERSION.RELEASE,
            "deviceModel" to Build.MODEL,
            "deviceManufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND,
            "locale" to Locale.getDefault().toString(),
            "eventName" to eventName
        )
    }

    private fun tryReadProjectToken(): String {
        return ExponeaContextProvider.applicationContext?.let {
            ExponeaConfigRepository.get(it)?.projectToken
        } ?: ""
    }

    private fun buildSentryReleaseInfo(): String {
        return "${appInfo.packageName}@${appInfo.versionName} - SDK ${BuildConfig.EXPONEA_VERSION_NAME}"
    }
}
