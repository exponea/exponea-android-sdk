package com.exponea.sdk.util

import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private var mainThreadDispatcherOverride: CoroutineScope? = null
private val lazyMainThreadDispatcher by lazy { CoroutineScope(Dispatchers.Main) }

private var backgroundThreadDispatcherOverride: CoroutineScope? = null
private val lazyBackgroundThreadDispatcher by lazy { CoroutineScope(Dispatchers.Default) }

internal var mainThreadDispatcher: CoroutineScope
    get() = mainThreadDispatcherOverride ?: lazyMainThreadDispatcher
    set(value) {
        mainThreadDispatcherOverride = value
    }

internal var backgroundThreadDispatcher: CoroutineScope
    get() = backgroundThreadDispatcherOverride ?: lazyBackgroundThreadDispatcher
    set(value) {
        backgroundThreadDispatcherOverride = value
    }

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
