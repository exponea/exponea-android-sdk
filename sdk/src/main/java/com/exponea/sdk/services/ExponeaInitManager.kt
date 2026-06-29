package com.exponea.sdk.services

import com.exponea.sdk.Exponea
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.logOnException
import java.util.concurrent.ConcurrentLinkedQueue

internal class ExponeaInitManager {

    val afterInitCallbacks = ConcurrentLinkedQueue<() -> Unit>()

    /**
     * Runs all pending callbacks that are waiting for ExponeaSDK init finish.
     */
    fun notifyInitializedState() {
        do {
            val pendingCallback = afterInitCallbacks.poll()
            pendingCallback?.let { executeCallbackSafely(it) }
        } while (pendingCallback != null)
        Logger.v(this, "All pending callbacks invoked")
    }

    /**
     * Drops all pending callbacks that are waiting for ExponeaSDK init finish.
     * This method should be invoked to release resources but used only in reasonable cases (such as anonymize).
     */
    fun clear() {
        if (afterInitCallbacks.isNotEmpty()) {
            Logger.e(this, "Removing ${afterInitCallbacks.size} pending callbacks of afterInit")
        }
        afterInitCallbacks.clear()
    }

    /**
     * Runs `afterInitBlock` code after ExponeaSDK is fully initialized.
     * If SDK is already initialized so is code invoked immediately.
     * !!! Pending callbacks are cleared on `anonymize` call.
     */
    fun waitForInitialize(
        afterInitBlock: () -> Unit
    ) {
        if (Exponea.isStopped) {
            Logger.e(this, "Method has not been invoked, SDK is stopping")
            return
        }
        Exponea.requireInitialized(notInitializedBlock = {
            afterInitCallbacks.add(afterInitBlock)
        }, initializedBlock = {
            executeCallbackSafely(afterInitBlock)
        })
    }

    /**
     * Like [waitForInitialize], but when the SDK is not yet initialized it defers quietly instead of
     * routing through [Exponea.requireInitialized], which logs an error. Use for callers that may
     * legitimately run before init() completes, where a deferral is expected rather than misuse.
     */
    fun runAfterInit(block: () -> Unit) {
        if (Exponea.isStopped) {
            Logger.e(this, "Skipping execution, SDK is stopping")
            return
        }
        if (Exponea.isInitialized) {
            executeCallbackSafely(block)
        } else {
            Logger.v(
                this,
                "SDK is not initialized yet, deferring execution until initialization finishes"
            )
            afterInitCallbacks.add(block)
        }
    }

    private fun executeCallbackSafely(block: () -> Unit) {
        runCatching(block).logOnException()
    }
}
