package com.exponea.sdk.util

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.Date
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

internal fun Call.enqueue(
    onResponse: (Call, Response) -> Unit,
    onFailure: (Call, IOException) -> Unit
) {
    this.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure(call, e)
        }

        override fun onResponse(call: Call, response: Response) {
            onResponse(call, response)
        }
    })
}

internal fun Context.addAppStateCallbacks(onOpen: () -> Unit, onClosed: () -> Unit) {
    (this as Application).registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
        private var activityCount: Int = 0
        override fun onActivityResumed(activity: Activity?) {
            runCatching {
                onOpen()
            }.logOnException()
            activityCount++
        }

        override fun onActivityStarted(activity: Activity?) {}
        override fun onActivityDestroyed(activity: Activity?) {}
        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
        override fun onActivityStopped(activity: Activity?) {}
        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
        override fun onActivityPaused(activity: Activity?) {
            activityCount--
            if (activityCount <= 0) {
                runCatching {
                    onClosed()
                }.logOnException()
            }
        }
    })
    this.registerComponentCallbacks(object : ComponentCallbacks2 {
        override fun onLowMemory() {}

        override fun onConfigurationChanged(newConfig: Configuration?) {}

        override fun onTrimMemory(level: Int) {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                runCatching {
                    onClosed()
                }.logOnException()
            }
        }
    })
}

internal fun Double.toDate(): Date {
    return Date((this * 1000).toLong())
}

internal fun currentTimeSeconds(): Double {
    return Date().time / 1000.0
}

internal inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

internal fun String?.adjustUrl(): String? {
    return if (this != null) {
        if (!contains("://")) {
            "http://$this"
        } else this
    } else {
        null
    }
}

fun Intent?.isDeeplinkIntent(): Boolean {
    return Intent.ACTION_VIEW == this?.action && data?.toString()?.startsWith("http", true) == true
}

fun <T> Result<T>.returnOnException(mapThrowable: (e: Throwable) -> T): T {
    return this.getOrElse {
        try {
            Logger.e(Exponea, "Unhandled error occurs", it)
        } catch (e: Throwable) {
            // cannot log problem, swallowing
        }
        if (Exponea.safeModeEnabled) {
            // `function` is internal and has to return T value
            // if error occurs here, let throw it, nothing more we can do
            return mapThrowable(it)
        } else {
            throw it
        }
    }
}

fun Result<Unit>.logOnException() {
    val exception = this.exceptionOrNull()
    if (exception != null) {
        try {
            Logger.e(Exponea, "Unhandled error occurs")
        } catch (e: Throwable) {
            // cannot log problem, swallowing
        }
        if (!Exponea.safeModeEnabled) {
            throw exception
        }
    }
}
