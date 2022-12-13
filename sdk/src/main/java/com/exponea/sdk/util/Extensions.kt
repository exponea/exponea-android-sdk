package com.exponea.sdk.util

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.exponea.sdk.Exponea
import com.google.gson.Gson
import com.google.gson.JsonElement
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
    val application = this.applicationContext as Application
    application.registerActivityLifecycleCallbacks(object :
            Application.ActivityLifecycleCallbacks {
        private var activityCount: Int = 0
        override fun onActivityResumed(activity: Activity) {
            runCatching {
                onOpen()
            }.logOnException()
            activityCount++
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityPaused(activity: Activity) {
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

        override fun onConfigurationChanged(newConfig: Configuration) {}

        override fun onTrimMemory(level: Int) {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                runCatching {
                    onClosed()
                }.logOnException()
            }
        }
    })
}

internal fun Context.getAppVersion(context: Context): String {
    try {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName
    } catch (e: Exception) {
        Logger.w(this, "Unable to get app version from package manager.")
    }
    return ""
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

fun Intent?.isViewUrlIntent(schemePrefix: String): Boolean {
    return Intent.ACTION_VIEW == this?.action && data?.scheme?.startsWith(schemePrefix, true) == true
}

fun <T> Result<T>.returnOnException(mapThrowable: (e: Throwable) -> T): T {
    return this.getOrElse {
        try {
            Logger.e(Exponea, "Exponea Safe Mode wrapper caught unhandled error", it)
        } catch (e: Throwable) {
            // cannot log problem, swallowing
        }
        if (Exponea.safeModeEnabled) {
            Exponea.telemetry?.reportCaughtException(it)
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
            Logger.e(Exponea, "Exponea Safe Mode wrapper caught unhandled error", exception)
        } catch (e: Throwable) {
            // cannot log problem, swallowing
        }
        if (!Exponea.safeModeEnabled) {
            throw exception
        } else {
            Exponea.telemetry?.reportCaughtException(exception)
        }
    }
}

fun <T> Result<T>.logOnException(): Result<T> {
    val exception = this.exceptionOrNull()
    if (exception != null) {
        try {
            Logger.e(Exponea, "Exponea Safe Mode wrapper caught unhandled error", exception)
        } catch (e: Throwable) {
            // cannot log problem, swallowing
        }
        if (!Exponea.safeModeEnabled) {
            throw exception
        } else {
            Exponea.telemetry?.reportCaughtException(exception)
        }
    }
    return this
}

val JsonElement.asOptionalString: String?
    get() = if (this.isJsonNull) null else this.asString

@Suppress("DEPRECATION")
fun View.setBackgroundColor(@DrawableRes backgroundId: Int, color: Int) {
    val drawable = if (Build.VERSION.SDK_INT >= 21)
        context.resources.getDrawable(backgroundId, null)
    else context.resources.getDrawable(backgroundId)
    drawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY)
    background = drawable
}

fun Context.isResumedActivity(): Boolean = runCatching {
    if (this !is Activity) return false
    val lifecycleOwner = this as? LifecycleOwner ?: return false
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
}.returnOnException { false }

fun Context.isReactNativeSDK(): Boolean {
    return isOtherSDK("ExponeaReactNativeSDK")
}

fun Context.isCapacitorSDK(): Boolean {
    return isOtherSDK("ExponeaCapacitorSDK")
}

fun Context.isFlutterSDK(): Boolean {
    return isOtherSDK("ExponeaFlutterSDK")
}

fun Context.isXamarinSDK(): Boolean {
    return isOtherSDK("ExponeaXamarinSDK")
}

fun Context.getReactNativeSDKVersion(): String? {
    return getSDKVersion("ExponeaReactNativeSDKVersion")
}

fun Context.getCapacitorSDKVersion(): String? {
    return getSDKVersion("ExponeaCapacitorSDKVersion")
}

fun Context.getFlutterSDKVersion(): String? {
    return getSDKVersion("ExponeaFlutterSDKVersion")
}

fun Context.getXamarinSDKVersion(): String? {
    return getSDKVersion("ExponeaXamarinSDKVersion")
}

private fun Context.isOtherSDK(sdk: String): Boolean = runCatching {
    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    if (appInfo.metaData == null) return false
    return appInfo.metaData[sdk] == true
}.returnOnException { false }

fun Context.isCalledFromExampleApp(): Boolean = runCatching {
    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    if (appInfo.metaData == null) return false
    return appInfo.metaData["ExponeaExampleApp"] == true
}.returnOnException { false }

private fun Context.getSDKVersion(metadataName: String): String? = runCatching {
    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    if (appInfo.metaData == null) return null
    return appInfo.metaData[metadataName] as String?
}.returnOnException { null }

public inline fun runOnMainThread(crossinline block: () -> Unit) {
    Handler(Looper.getMainLooper()).post {
        block.invoke()
    }
}
