package com.exponea.sdk.util

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

internal class ThreadSafeAccess {

    companion object {
        private const val LOCK = "GATE"
    }

    fun waitForAccess(action: () -> Unit) {
        runCatching {
            synchronized(LOCK) {
                executeSafely(action)
            }
        }.logOnException()
    }

    fun <T> waitForAccessWithResult(action: () -> T): Result<T> {
        return runCatching {
            synchronized(LOCK) {
                return executeSafelyWithResult(action)
            }
        }.logOnExceptionWithResult()
    }

    fun waitForAccessWithDone(action: (done: () -> Unit) -> Unit) {
        runCatching {
            synchronized(LOCK) {
                val timeout = CountDownLatch(1)
                action.invoke {
                    timeout.countDown()
                }
                if (!timeout.await(10, SECONDS)) {
                    Logger.e(this, "ThreadAccess freed prematurely")
                }
            }
        }.logOnException()
    }

    private fun executeSafely(action: () -> Unit) {
        runCatching(action).logOnException()
    }

    private fun <T> executeSafelyWithResult(action: () -> T): Result<T> {
        return runCatching(action).logOnExceptionWithResult()
    }
}
