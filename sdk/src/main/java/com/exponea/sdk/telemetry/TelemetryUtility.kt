package com.exponea.sdk.telemetry

import com.exponea.sdk.telemetry.model.ErrorData
import com.exponea.sdk.telemetry.model.ErrorStackTraceElement
import kotlin.math.min

object TelemetryUtility {
    private const val SDK_PACKAGE = "com.exponea"
    private const val MAX_STACK_TRACE_LENGTH = 100

    internal fun getErrorData(e: Throwable): ErrorData {
        return getErrorDataInternal(e, hashSetOf())
    }

    /**
     * There can be loops in cause hierarchy
     * We need to remember which throwables we already visited and break in case of a loop
     */
    private fun getErrorDataInternal(e: Throwable, visitedThrowables: HashSet<Throwable>): ErrorData {
        visitedThrowables.add(e)
        var cause: ErrorData? = null
        if (e.cause != null) {
            val throwableCause = e.cause as Throwable
            if (!visitedThrowables.contains(throwableCause)) {
                cause = getErrorDataInternal(throwableCause, visitedThrowables)
            }
        }
        val stackTrace = e.stackTrace.slice(0 until min(MAX_STACK_TRACE_LENGTH, e.stackTrace.size)).map {
            ErrorStackTraceElement(it.className, it.methodName, it.fileName, it.lineNumber)
        }

        return ErrorData(
            type = e.javaClass.name,
            message = e.message ?: "",
            stackTrace = stackTrace,
            cause = cause
        )
    }

    internal fun isSDKRelated(e: Throwable): Boolean {
        var t: Throwable? = e
        var visited = hashSetOf<Throwable>()
        while (t != null && !visited.contains(t)) {
            t.stackTrace.forEach { if (it.className.startsWith(SDK_PACKAGE)) return true }
            visited.add(t)
            t = t.cause
        }
        return false
    }
}