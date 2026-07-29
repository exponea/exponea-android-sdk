package com.exponea.sdk.util

import android.util.Log
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.LoggerCallback
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CancellationException

object Logger {
    enum class Level(var value: Int) {
        OFF(5),
        ERROR(4),
        WARN(3),
        INFO(2),
        DEBUG(1),
        VERBOSE(0)
    }

    var level: Level = Constants.Logger.defaultLoggerLevel

    private val loggerCallbacks = CopyOnWriteArrayList<LoggerCallback>()

    internal fun registerCallback(callback: LoggerCallback) {
        loggerCallbacks.addIfAbsent(callback)
    }

    internal fun unregisterCallback(callback: LoggerCallback) {
        loggerCallbacks.remove(callback)
    }

    @InternalLoggerApi
    fun e(parent: Any, message: String) {
        Exponea.telemetry?.reportLog(parent, message)
        if (level.value <= Level.ERROR.value) {
            Log.e(parent.javaClass.simpleName, message)
        }
        notifyCallbacks(Level.ERROR, message, null)
    }

    @InternalLoggerApi
    fun e(parent: Any, message: String, throwable: Throwable) {
        Exponea.telemetry?.reportLog(parent, message)
        if (level.value <= Level.ERROR.value) {
            Log.e(parent.javaClass.simpleName, message, throwable)
        }
        notifyCallbacks(Level.ERROR, message, throwable)
    }

    @InternalLoggerApi
    fun w(parent: Any, message: String) {
        Exponea.telemetry?.reportLog(parent, message)
        if (level.value <= Level.WARN.value) {
            Log.w(parent.javaClass.simpleName, message)
        }
        notifyCallbacks(Level.WARN, message, null)
    }

    @InternalLoggerApi
    fun i(parent: Any, message: String) {
        Exponea.telemetry?.reportLog(parent, message)
        if (level.value <= Level.INFO.value) {
            Log.i(parent.javaClass.simpleName, message)
        }
        notifyCallbacks(Level.INFO, message, null)
    }

    @InternalLoggerApi
    fun d(parent: Any, message: String) {
        Exponea.telemetry?.reportLog(parent, message)
        if (level.value <= Level.DEBUG.value) {
            Log.d(parent.javaClass.simpleName, message)
        }
        notifyCallbacks(Level.DEBUG, message, null)
    }

    @InternalLoggerApi
    fun v(parent: Any, message: String) {
        Exponea.telemetry?.reportLog(parent, message)
        if (level.value <= Level.VERBOSE.value) {
            Log.v(parent.javaClass.simpleName, message)
        }
        notifyCallbacks(Level.VERBOSE, message, null)
    }

    private fun notifyCallbacks(level: Level, message: String, throwable: Throwable?) {
        loggerCallbacks.forEach {
            try {
                it.onLog(level, message, throwable)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Must not re-log through Logger here — that would re-enter dispatch (see onLog contract).
                Log.e("LoggerCallback", "Logger callback failed", e)
            }
        }
    }
}
