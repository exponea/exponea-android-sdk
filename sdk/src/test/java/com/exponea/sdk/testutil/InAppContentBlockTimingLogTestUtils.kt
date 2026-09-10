package com.exponea.sdk.testutil

import com.exponea.sdk.models.LoggerCallback
import com.exponea.sdk.util.Logger
import java.util.Collections
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class RecordingLoggerCallback : LoggerCallback {
    val logs = Collections.synchronizedList(mutableListOf<Pair<Logger.Level, String>>())

    override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
        logs.add(level to message)
    }
}

internal fun latestInAppContentBlockTimingLog(
    callback: RecordingLoggerCallback,
    source: String? = null,
    etagMode: String? = null
): Pair<Logger.Level, String> {
    return callback.logs.asReversed().firstOrNull { (_, message) ->
        if (!message.startsWith("InAppCB timing: ")) {
            false
        } else {
            val fields = parseInAppContentBlockTimingLog(message)
            (source == null || fields["source"] == source) &&
                (etagMode == null || fields["etagMode"] == etagMode)
        }
    }
        ?: throw AssertionError("InAppCB timing log was not emitted")
}

internal fun parseInAppContentBlockTimingLog(message: String): Map<String, String> {
    val prefix = "InAppCB timing: "
    assertTrue(message.startsWith(prefix), "Unexpected timing log: $message")
    return message.removePrefix(prefix)
        .split(", ")
        .associate {
            val separatorIndex = it.indexOf('=')
            assertTrue(separatorIndex > 0, "Timing field is missing '=': $it")
            it.substring(0, separatorIndex) to it.substring(separatorIndex + 1)
        }
}

internal fun assertNonNegativeTimingFields(fields: Map<String, String>, vararg names: String) {
    names.forEach { name ->
        val value = assertNotNull(fields[name], "Missing timing field $name")
        assertTrue(value.toLong() >= 0, "Timing field $name should be non-negative, was $value")
    }
}
