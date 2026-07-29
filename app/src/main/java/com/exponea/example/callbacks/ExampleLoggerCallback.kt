package com.exponea.example.callbacks

import android.util.Log
import com.exponea.sdk.models.LoggerCallback
import com.exponea.sdk.util.Logger

/**
 * Dummy [com.exponea.sdk.models.LoggerCallback] used only to demonstrate the API. It does nothing useful —
 * it simply re-logs the SDK's warning and error messages back to logcat.
 */
object ExampleLoggerCallback : LoggerCallback {
    private val tag = this::class.simpleName

    override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
        when (level) {
            Logger.Level.WARN -> Log.w(tag, message)
            Logger.Level.ERROR ->
                throwable?.let { Log.e(tag, message, it) } ?: Log.e(tag, message)

            else -> Unit // ignore verbose/debug/info
        }
    }
}
