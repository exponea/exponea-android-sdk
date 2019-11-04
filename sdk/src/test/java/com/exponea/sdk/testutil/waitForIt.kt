package com.exponea.sdk.testutil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

const val DEFAULT_TIMEOUT = 5000L

/**
 * Runs lambda and blocks the thread until lambda invokes it's parameter or timeout runs out.
 * Be careful not to block main thread in lambda function, otherwise timeout will be ignored.
 *
 * JUnit is only catching assertion errors on it's own thread.
 * Asserts in different threads raise exception in that thread, test won't fail by default.
 * When an assert fails, we remember the error, stop waiting and rethrow the error on original thread.
 *
 * Examples:
 * waitForIt {
 *     thread {
 *         Thread.sleep(1000)
 *         it()
 *     }
 * }
 *
 * waitForIt {
 *     thread {
 *         it.assertTrue(false) // causes test to fail, normal assertTrue wouldn't
 *         it()
 *     }
 * }
 */
internal fun waitForIt(
    timeoutMS: Long = DEFAULT_TIMEOUT,
    lambda: (ThreadAssertionCollector) -> Unit
) {
    ThreadAssertionCollector(timeoutMS, lambda)
}

internal class ThreadAssertionCollector(
    private val timeoutMS: Long = DEFAULT_TIMEOUT,
    private val lambda: (ThreadAssertionCollector) -> Unit
) {
    private val count = CountDownLatch(1)
    private var error: AssertionError? = null

    init { start() }

    fun assertNull(actual: Any?, message: String? = null) {
        assertCatching { kotlin.test.assertNull(actual, message) }
    }

    fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
        return assertCatching { kotlin.test.assertNotNull(actual, message) }
    }

    fun assertTrue(actual: Boolean, message: String? = null) {
        assertCatching { kotlin.test.assertTrue(actual, message) }
    }

    fun assertFalse(actual: Boolean, message: String? = null) {
        assertCatching { kotlin.test.assertFalse(actual, message) }
    }

    fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
        assertCatching { kotlin.test.assertEquals(expected, actual, message) }
    }

    fun <T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
        assertCatching { kotlin.test.assertNotEquals(illegal, actual, message) }
    }

    operator fun invoke() {
        count.countDown()
    }

    private fun <T> assertCatching(assertBlock: () -> T): T {
        try {
            return assertBlock()
        } catch (e: AssertionError) {
            error = e
            count.countDown()
            throw e
        }
    }

    private fun start() {
        lambda(this)
        if (!count.await(timeoutMS, TimeUnit.MILLISECONDS)) {
            throw TimeoutException("Operation did not finish in $timeoutMS milliseconds.")
        }
        if (error != null) {
            throw error!!
        }
    }
}
