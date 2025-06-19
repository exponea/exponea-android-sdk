package com.exponea.sdk.telemetry.upload

import com.google.gson.annotations.SerializedName

internal data class ExponeaSentryEnvelope(
    val header: ExponeaSentryEnvelopeHeader,
    val item: ExponeaSentryEnvelopeItem
) {
    companion object
}

internal data class ExponeaSentryEnvelopeHeader(
    @SerializedName("event_id")
    val eventId: String?,
    @SerializedName("dsn")
    val dsn: String,
    @SerializedName("sent_at")
    val sentAt: String
)

internal data class ExponeaSentryEnvelopeItem(
    val header: ExponeaSentryEnvelopeItemHeader,
    val body: ExponeaSentryEnvelopeItemBody
)

internal data class ExponeaSentryEnvelopeItemHeader(
    @SerializedName("type")
    val type: String,
    @SerializedName("length")
    val length: Int,
    @SerializedName("content_type")
    val contentType: String = "application/json"
) {
    fun withLength(newLength: Int): ExponeaSentryEnvelopeItemHeader {
        return ExponeaSentryEnvelopeItemHeader(
            type = type,
            length = newLength,
            contentType = contentType
        )
    }
}

internal interface ExponeaSentryEnvelopeItemBody {
    /**
     * Returns header with type and contentType. Length has to be calculated after JSON serialisation.
     */
    fun prepareHeader(): ExponeaSentryEnvelopeItemHeader
}

internal data class ExponeaSentryMessage(
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("message")
    val message: ExponeaSentryMessageLog,
    @SerializedName("logger")
    val logger: String = "messageLogger",
    @SerializedName("level")
    val level: String = "info",
    @SerializedName("fingerprint")
    val fingerprint: List<String> = listOf("metrics"),
    @SerializedName("event_id")
    val eventId: String,
    @SerializedName("contexts")
    val contexts: ExponeaSentryContext,
    @SerializedName("tags")
    val tags: Map<String, String>,
    @SerializedName("release")
    val release: String,
    @SerializedName("environment")
    val environment: String,
    @SerializedName("platform")
    val platform: String = "java",
    @SerializedName("extra")
    val extra: Map<String, String>
) : ExponeaSentryEnvelopeItemBody {
    override fun prepareHeader(): ExponeaSentryEnvelopeItemHeader {
        return ExponeaSentryEnvelopeItemHeader(
            type = "event",
            length = 0
        )
    }
}

internal data class ExponeaSentryMessageLog(
    @SerializedName("formatted")
    val formatted: String
)

internal data class ExponeaSentryContext(
    @SerializedName("app")
    val app: ExponeaSentryAppContextInfo,
    @SerializedName("device")
    val device: ExponeaSentryDeviceContextInfo,
    @SerializedName("os")
    val os: ExponeaSentryOsContextInfo
)

internal data class ExponeaSentryAppContextInfo(
    @SerializedName("type")
    val type: String = "app",
    @SerializedName("app_identifier")
    val appIdentifier: String,
    @SerializedName("app_name")
    val appName: String,
    @SerializedName("app_build")
    val appBuild: String
)

internal data class ExponeaSentryDeviceContextInfo(
    @SerializedName("type")
    val type: String = "device",
    @SerializedName("model")
    val model: String,
    @SerializedName("manufacturer")
    val manufacturer: String,
    @SerializedName("brand")
    val brand: String
)

internal data class ExponeaSentryOsContextInfo(
    @SerializedName("type")
    val type: String = "os",
    @SerializedName("name")
    val name: String = "Android",
    @SerializedName("version")
    val version: String
)

internal data class ExponeaSentryException(
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("logger")
    val logger: String = "errorLogger",
    @SerializedName("threads")
    val threads: ExponeaSentryValuesWrapper<ExponeaSentryThread>,
    @SerializedName("exception")
    val exception: ExponeaSentryValuesWrapper<ExponeaSentryExceptionPart>,
    @SerializedName("level")
    val level: String,
    @SerializedName("fingerprint")
    val fingerprint: List<String> = listOf("error"),
    @SerializedName("event_id")
    val eventId: String,
    @SerializedName("contexts")
    val contexts: ExponeaSentryContext,
    @SerializedName("tags")
    val tags: Map<String, String>,
    @SerializedName("release")
    val release: String,
    @SerializedName("environment")
    val environment: String,
    @SerializedName("platform")
    val platform: String = "java",
    @SerializedName("extra")
    val extra: Map<String, String>
) : ExponeaSentryEnvelopeItemBody {
    override fun prepareHeader(): ExponeaSentryEnvelopeItemHeader {
        return ExponeaSentryEnvelopeItemHeader(
            type = "event",
            length = 0
        )
    }
}

internal data class ExponeaSentryExceptionPart(
    @SerializedName("type")
    val type: String,
    @SerializedName("value")
    val value: String,
    @SerializedName("stacktrace")
    val stacktrace: ExponeaSentryStackTrace,
    @SerializedName("mechanism")
    val mechanism: ExponeaSentryExceptionMechanism,
    @SerializedName("thread_id")
    val threadId: Long
)

internal data class ExponeaSentryExceptionMechanism(
    @SerializedName("type")
    val type: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("handled")
    val handled: Boolean
)

internal data class ExponeaSentryThread(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("crashed")
    val crashed: Boolean,
    @SerializedName("current")
    val current: Boolean,
    @SerializedName("daemon")
    val daemon: Boolean,
    @SerializedName("main")
    val main: Boolean,
    @SerializedName("stacktrace")
    val stacktrace: ExponeaSentryStackTrace
)

internal data class ExponeaSentryStackTrace(
    @SerializedName("frames")
    val frames: List<ExponeaSentryStackFrame>
)

internal data class ExponeaSentryStackFrame(
    @SerializedName("filename")
    val filename: String?,
    @SerializedName("function")
    val function: String?,
    @SerializedName("module")
    val module: String?,
    @SerializedName("lineno")
    val lineno: Int
)

internal data class ExponeaSentryValuesWrapper<T>(
    @SerializedName("values")
    val values: List<T>
)

internal data class ExponeaSentrySession(
    @SerializedName("started")
    val started: String,
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("did")
    val distinctId: String,
    @SerializedName("sid")
    val sessionId: String,
    @SerializedName("init")
    val init: Boolean = true,
    @SerializedName("status")
    val status: String = "ok",
    @SerializedName("seq")
    val sequence: Long,
    @SerializedName("attrs")
    val attributes: ExponeaSentryAttributes,
    @SerializedName("extra")
    val extra: Map<String, Any>
) : ExponeaSentryEnvelopeItemBody {
    override fun prepareHeader(): ExponeaSentryEnvelopeItemHeader {
        return ExponeaSentryEnvelopeItemHeader(
            type = "session",
            length = 0
        )
    }
}

internal data class ExponeaSentryAttributes(
    @SerializedName("release")
    val release: String,
    @SerializedName("environment")
    val environment: String
)
