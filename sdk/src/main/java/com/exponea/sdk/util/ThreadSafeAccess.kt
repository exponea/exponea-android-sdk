package com.exponea.sdk.util

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger

internal class ThreadSafeAccess {

    companion object {
        private val SINGLETON_LOCKS_ACCESS = ThreadSafeAccess()
        private val SINGLETONS = mutableMapOf<String, ThreadSafeAccess>()
        private fun getSingletonAccess(lockName: String): ThreadSafeAccess {
            return SINGLETON_LOCKS_ACCESS.waitForAccessWithResult {
                SINGLETONS.getOrPut(lockName) { ThreadSafeAccess() }
            }.getOrThrow()
        }
        private fun removeUnusedSingletonAccess(lockName: String) {
            SINGLETON_LOCKS_ACCESS.waitForAccess {
                val access = SINGLETONS[lockName]
                if (access != null && access.awaitingCount.get() <= 0) {
                    SINGLETONS.remove(lockName)
                }
            }
        }
        /**
         * Creates a singleton ThreadSafeAccess for 'lockName' so all threads are locked on access for same 'lockName'.
         */
        fun waitForAccess(lockName: String, action: () -> Unit) {
            getSingletonAccess(lockName).waitForAccess(action)
            removeUnusedSingletonAccess(lockName)
        }

        /**
         * Creates a singleton ThreadSafeAccess for 'lockName' so all threads are locked on access for same 'lockName'.
         */
        fun <T> waitForAccessWithResult(lockName: String, action: () -> T): Result<T> {
            val result = getSingletonAccess(lockName).waitForAccessWithResult(action)
            removeUnusedSingletonAccess(lockName)
            return result
        }

        /**
         * Creates a singleton ThreadSafeAccess for 'lockName' so all threads are locked on access for same 'lockName'.
         * To release the lock use the 'done' callback.
         */
        fun waitForAccessWithDone(lockName: String, action: (done: () -> Unit) -> Unit) {
            getSingletonAccess(lockName).waitForAccessWithDone(action)
            removeUnusedSingletonAccess(lockName)
        }

        /**
         * Test validation purpose
         */
        internal fun hasLock(lockName: String): Boolean {
            return SINGLETONS.contains(lockName)
        }
    }

    private val awaitingCount = AtomicInteger(0)

    fun waitForAccess(action: () -> Unit) {
        awaitingCount.incrementAndGet()
        runCatching {
            synchronized(this@ThreadSafeAccess) {
                executeSafely(action)
            }
        }.logOnException()
        awaitingCount.decrementAndGet()
    }

    fun <T> waitForAccessWithResult(action: () -> T): Result<T> {
        awaitingCount.incrementAndGet()
        return runCatching {
            synchronized(this@ThreadSafeAccess) {
                return executeSafelyWithResult(action)
            }
        }.logOnExceptionWithResult().also {
            awaitingCount.decrementAndGet()
        }
    }

    fun waitForAccessWithDone(action: (done: () -> Unit) -> Unit) {
        awaitingCount.incrementAndGet()
        runCatching {
            synchronized(this@ThreadSafeAccess) {
                val timeout = CountDownLatch(1)
                action.invoke {
                    timeout.countDown()
                }
                if (!timeout.await(10, SECONDS)) {
                    Logger.e(this, "ThreadAccess freed prematurely")
                }
            }
        }.logOnException()
        awaitingCount.decrementAndGet()
    }

    private fun executeSafely(action: () -> Unit) {
        runCatching(action).logOnException()
    }

    private fun <T> executeSafelyWithResult(action: () -> T): Result<T> {
        return runCatching(action).logOnExceptionWithResult()
    }
}
