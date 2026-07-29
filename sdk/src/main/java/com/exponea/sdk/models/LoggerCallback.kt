package com.exponea.sdk.models

import com.exponea.sdk.util.Logger

/**
 * Observes log lines produced by the SDK's [Logger].
 *
 * Register an implementation with [com.exponea.sdk.Exponea.registerLoggerCallback] to forward SDK
 * logs into your own logging pipeline or crash-reporting tool. This is a read-only side channel: it
 * cannot modify the message, cannot suppress the SDK's own console output, and cannot stop other
 * observers.
 *
 * Callbacks are dispatched unconditionally, independent of [com.exponea.sdk.Exponea.loggerLevel] — you observe every
 * SDK log line regardless of the configured console verbosity, including logs emitted while the SDK is stopped.
 */
interface LoggerCallback {
    /**
     * Invoked for every SDK log line.
     *
     * Implementations are invoked **synchronously on the thread that produced the log**, which may be
     * the app's main thread, and at high frequency. Therefore:
     *
     * - **Do not perform blocking work** inside [onLog]. If you need to forward a log elsewhere, hand
     *   the data off (enqueue, buffer, or post to another thread) rather than doing the work here.
     * - **Do not call back into the SDK's [Logger]** (or any SDK code that logs) from inside [onLog].
     *   Dispatch is synchronous and unconditional, so logging through [Logger] here — directly or
     *   transitively — re-enters dispatch and produces unbounded recursion.
     *
     * @param level the severity of the log line.
     * @param message the log message.
     * @param throwable the associated exception, or `null` (only error logs may carry one).
     */
    fun onLog(level: Logger.Level, message: String, throwable: Throwable?)
}
