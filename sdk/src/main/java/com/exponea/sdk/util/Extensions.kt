package com.exponea.sdk.util

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.PushNotificationDelegate
import com.exponea.sdk.models.PushOpenedData
import com.exponea.sdk.services.MessagingUtils
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        return packageInfo?.versionName ?: ""
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
    return isViewUrlIntent() && this?.data?.scheme?.startsWith(schemePrefix, true) == true
}

internal fun Intent?.isViewUrlIntent(): Boolean {
    if (this == null) {
        return false
    }
    if (Intent.ACTION_VIEW != this.action) {
        Logger.v(this, "Only '${Intent.ACTION_VIEW}' is allowed for ViewUrl intent")
        return false
    }
    if (scheme.isNullOrBlank()) {
        Logger.v(this, "Deeplinks and Android applinks must contains scheme")
        return false
    }
    return true
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

fun <T> Result<T>.logOnExceptionWithResult(): Result<T> {
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
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
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

fun Context.isMauiSDK(): Boolean {
    return isOtherSDK("BloomreachMauiSDK")
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

fun Context.getMauiSDKVersion(): String? {
    return getSDKVersion("BloomreachMauiSDKVersion")
}

private fun Context.isOtherSDK(sdk: String): Boolean = runCatching {
    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    if (appInfo.metaData == null) {
        return false
    }
    return appInfo.metaData.getBoolean(sdk, false)
}.returnOnException { false }

fun Context.isCalledFromExampleApp(): Boolean = runCatching {
    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    if (appInfo.metaData == null) {
        return false
    }
    return appInfo.metaData.getBoolean("ExponeaExampleApp", false)
}.returnOnException { false }

private fun Context.getSDKVersion(metadataName: String): String? = runCatching {
    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    if (appInfo.metaData == null) return null
    return appInfo.metaData[metadataName] as String?
}.returnOnException { null }

internal var mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
internal var backgroundThreadDispatcher = CoroutineScope(Dispatchers.Default)

internal inline fun runOnMainThread(crossinline block: () -> Unit): Job {
    return mainThreadDispatcher.launch {
        runCatching {
            block.invoke()
        }.logOnException()
    }
}

internal inline fun runOnMainThread(delayMillis: Long, crossinline block: () -> Unit): Job {
    return mainThreadDispatcher.launch {
        runCatching {
            try {
                delay(delayMillis)
            } catch (e: Exception) {
                Logger.w(this, "Delayed task has been cancelled: ${e.localizedMessage}")
                return@runCatching
            }
            block.invoke()
        }.logOnException()
    }
}

internal inline fun runOnBackgroundThread(crossinline block: () -> Unit): Job {
    return backgroundThreadDispatcher.launch {
        runCatching {
            block.invoke()
        }.logOnException()
    }
}

internal inline fun runOnBackgroundThread(
    delayMillis: Long,
    crossinline block: () -> Unit
): Job {
    return backgroundThreadDispatcher.launch {
        runCatching {
            try {
                delay(delayMillis)
            } catch (e: Exception) {
                Logger.w(this, "Delayed task has been cancelled: ${e.localizedMessage}")
                return@runCatching
            }
            block.invoke()
        }.logOnException()
    }
}

internal inline fun runOnBackgroundThread(
    delayMillis: Long,
    timeoutMillis: Long? = null,
    crossinline block: suspend () -> Unit,
    crossinline onTimeout: () -> Unit
): Job {
    var cancellerJob: Job? = null
    val backgroundJob = backgroundThreadDispatcher.launch {
        runCatching {
            try {
                delay(delayMillis)
            } catch (e: Exception) {
                Logger.w(this, "Delayed task has been cancelled: ${e.localizedMessage}")
                return@runCatching
            }
            block.invoke()
            cancellerJob?.cancel("Task finished successfully")
        }.logOnException()
    }
    cancellerJob = timeoutMillis?.let {
        backgroundThreadDispatcher.launch {
            runCatching {
                try {
                    delay(it)
                } catch (e: Exception) {
                    Logger.v(this, "Task cancellation stopped: ${e.localizedMessage}")
                    return@runCatching
                }
                backgroundJob.cancel("Task timed out after $it millis")
                onTimeout.invoke()
            }.logOnException()
        }
    }
    return backgroundJob
}

internal inline fun ensureOnBackgroundThread(crossinline block: () -> Unit) {
    if (isRunningOnUiThread()) {
        runOnBackgroundThread(block)
    } else {
        runCatching {
            block.invoke()
        }.logOnException()
    }
}

internal inline fun ensureOnMainThread(crossinline block: () -> Unit) {
    if (isRunningOnUiThread()) {
        runCatching {
            block.invoke()
        }.logOnException()
    } else {
        runOnMainThread(block)
    }
}

internal fun isRunningOnUiThread(): Boolean {
    return Looper.myLooper() == Looper.getMainLooper()
}

/**
 * Runs 'work' with 'timeout' limitation. If work takes too long, 'onExpire' is called.
 * Please ensure that 'onExpire' will be invoked fast as possible. There is no limitation applied.
 */
internal inline fun <T> runWithTimeout(
    timeoutMillis: Long,
    noinline work: () -> T,
    noinline onExpire: () -> T
): T {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return runWithTimeoutForApi24(timeoutMillis, work, onExpire)
    }
    return runWithTimeoutPreApi24(timeoutMillis, work, onExpire)
}

private fun <T> runWithTimeoutPreApi24(
    timeoutMillis: Long,
    work: () -> T,
    onExpire: () -> T
): T {
    try {
        return object : AsyncTask<Void, Int, T>() {
            override fun doInBackground(vararg params: Void?): T {
                return work()
            }
        }.execute().get(timeoutMillis, MILLISECONDS)
    } catch (e: Throwable) {
        return onExpire()
    }
}

@RequiresApi(Build.VERSION_CODES.N)
private fun <T> runWithTimeoutForApi24(
    timeoutMillis: Long,
    work: () -> T,
    onExpire: () -> T
): T {
    try {
        return CompletableFuture
            .supplyAsync(work)
            .get(timeoutMillis, MILLISECONDS)
    } catch (e: Throwable) {
        return onExpire()
    }
}

internal inline fun <reified T : Any> Map<String, Any?>.getRequired(key: String): T {
    return getSafely(key, T::class)
}

internal fun <T : Any> Map<String, Any?>.getSafely(key: String, type: KClass<T>): T {
    val value = this[key] ?: throw Exception("Property '$key' cannot be null.")
    if (value::class == type) {
        @Suppress("UNCHECKED_CAST")
        return value as T
    } else {
        throw Exception(
            "Incorrect type for key '$key'. Expected ${type.simpleName} got ${value::class.simpleName}"
        )
    }
}

internal inline fun <reified T : Any> Map<String, Any?>.getNullSafelyMap(
    key: String,
    defaultValue: Map<String, T>? = null
): Map<String, T>? {
    return getNullSafelyMap(key, T::class, defaultValue)
}

/**
 * Returns a map containing all key-value pairs with values are instances of specified class.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
internal fun <K, V, R> Map<out K, V>.filterValueIsInstance(klass: Class<R>): Map<K, R> {
    val result = LinkedHashMap<K, R>()
    for (entry in this) {
        if (klass.isInstance(entry.value)) {
            @Suppress("UNCHECKED_CAST")
            ((entry.value as R).also { result[entry.key] = it })
        }
    }
    return result
}

internal inline fun <reified T : Any> Map<String, Any?>.getNullSafelyMap(
    key: String,
    type: KClass<T>,
    defaultValue: Map<String, T>? = null
): Map<String, T>? {
    val value = this[key] ?: return defaultValue
    @Suppress("UNCHECKED_CAST")
    val mapOfAny = value as? Map<String, Any?> ?: throw Exception(
        "Non-map type for key '$key'. Got ${value::class.simpleName}"
    )
    return mapOfAny.filterValueIsInstance(type.java)
}

internal inline fun <reified T : Any> Map<String, Any?>.getNullSafelyArray(
    key: String,
    defaultValue: List<T>? = null
): List<T>? {
    return getNullSafelyArray(key, T::class, defaultValue)
}

internal inline fun <reified T : Any> Map<String, Any?>.getNullSafelyArray(
    key: String,
    type: KClass<T>,
    defaultValue: List<T>? = null
): List<T>? {
    val value = this[key] ?: return defaultValue
    val arrayOfAny = value as? List<Any?> ?: throw Exception(
        "Non-array type for key '$key'. Got ${value::class.simpleName}"
    )
    return arrayOfAny
        .filterIsInstance(type.java)
}

internal inline fun <reified T : Any> Map<String, Any?>.getNullSafely(key: String, defaultValue: T? = null): T? {
    return getNullSafely(key, T::class, defaultValue)
}

internal fun <T : Any> Map<String, Any?>.getNullSafely(key: String, type: KClass<T>, defaultValue: T? = null): T? {
    val value = this[key] ?: return defaultValue
    @Suppress("UNCHECKED_CAST")
    return (value as? T) ?: throw Exception(
        "Incorrect type for key '$key'. Expected ${type.simpleName} got ${value::class.simpleName}"
    )
}

internal fun Drawable?.applyTint(color: Int): Drawable? {
    if (this == null) return null
    val wrappedIcon: Drawable = DrawableCompat.wrap(this)
    DrawableCompat.setTint(wrappedIcon, color)
    return wrappedIcon
}

private val awaitingBlocks = ConcurrentHashMap<String, () -> Unit>()

/**
 * Runs block after initialization of SDK. Last block registered by blockId will be executed.
 */
internal fun runForInitializedSDK(
    blockId: String,
    afterInitBlock: () -> Unit
) {
    val isNewBlock = !awaitingBlocks.containsKey(blockId)
    awaitingBlocks[blockId] = afterInitBlock
    if (isNewBlock) {
        Exponea.initGate.waitForInitialize {
            awaitingBlocks[blockId]?.let {
                awaitingBlocks.remove(blockId)
                it.invoke()
            }
        }
    }
}

internal fun PushNotificationDelegate.handleReceivedPushUpdate(data: Map<String, Any>) {
    runOnMainThread {
        if (MessagingUtils.isSilentPush(data)) {
            this.onSilentPushNotificationReceived(data)
        } else {
            this.onPushNotificationReceived(data)
        }
    }
}

internal fun PushNotificationDelegate.handleClickedPushUpdate(data: PushOpenedData) {
    runOnMainThread {
        this.onPushNotificationOpened(
            data.actionType,
            data.actionUrl,
            data.extraData
        )
    }
}

internal fun InAppContentBlock.deepCopy(): InAppContentBlock {
    val sourceJson = ExponeaGson.instance.toJson(this)
    val target: InAppContentBlock = ExponeaGson.instance.fromJson(sourceJson)
    this.personalizedData?.let { sourcePersonData ->
        // if source has data, target must too
        target.personalizedData!!.loadedAt = Date(sourcePersonData.loadedAt!!.time)
    }
    return target
}
