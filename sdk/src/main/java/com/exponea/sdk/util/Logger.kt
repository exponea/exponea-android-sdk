package com.exponea.sdk.util

import android.util.Log

object Logger {
    enum class Level(var value: Int) {
        OFF(5),
        ERROR(4),
        WARN(3),
        INFO(2),
        DEBUG(1),
        VERBOSE(0)
    }

    var level: Level = Level.INFO

    fun e(parent: Any, message: String) {
        if (level.value >= Level.ERROR.value) {
            return
        }

        Log.e(parent.javaClass.simpleName, message)
    }

    fun e(parent: Any, message: String, exception: Exception) {
        if (level.value >= Level.ERROR.value) {
            return
        }

        Log.e(parent.javaClass.simpleName, message, exception)
    }

    fun w(parent: Any, message: String) {
        if (level.value > Level.WARN.value) {
            return
        }
        Log.w(parent.javaClass.simpleName, message)
    }

    fun i(parent: Any, message: String) {
        if (level.value > Level.INFO.value) {
            return
        }
        Log.i(parent.javaClass.simpleName, message)
    }

    fun d(parent: Any, message: String) {
        if (level.value > Level.DEBUG.value) {
            return
        }
        Log.d(parent.javaClass.simpleName, message)
    }

    fun v(parent: Any, message: String) {
        if (level.value > Level.VERBOSE.value) {
            return
        }
        Log.v(parent.javaClass.simpleName, message)
    }
}