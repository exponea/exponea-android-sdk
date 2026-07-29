package com.exponea.sdk.util

/**
 * Marks the [Logger] logging methods (`e`/`w`/`i`/`d`/`v`) as SDK-internal API.
 *
 * These methods are public today only for backward compatibility. Consumers should not call them.
 * Use your own logging for your own logs, and register a [com.exponea.sdk.models.LoggerCallback] to observe SDK logs.
 *
 * They are slated to be removed from the public API (made internal) in the next major release.
 */
@RequiresOptIn(
    message = "Logger.e/w/i/d/v are SDK-internal and will be removed from the public API in the next major release.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class InternalLoggerApi
