package com.exponea.sdk.util

import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.ThreadSafeAccess.Companion.waitForAccess
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class ThreadTests {

    @Test
    fun `keep background thread flow`() {
        val ranSteps = mutableListOf<Int>()
        waitForIt(500) {
            ranSteps.add(0)
            runOnBackgroundThread {
                ranSteps.add(2)
                ensureOnBackgroundThread {
                    ranSteps.add(3)
                    Thread.sleep(100)
                    ranSteps.add(4)
                    it()
                }
            }
            ranSteps.add(1)
        }
        ranSteps.add(5)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), ranSteps)
    }

    @Test
    fun `keep background thread flow with delay`() {
        val ranSteps = mutableListOf<Int>()
        waitForIt(500) {
            ranSteps.add(0)
            runOnBackgroundThread(100) {
                ranSteps.add(2)
                ensureOnBackgroundThread {
                    ranSteps.add(3)
                    Thread.sleep(100)
                    ranSteps.add(4)
                    it()
                }
            }
            ranSteps.add(1)
        }
        ranSteps.add(5)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), ranSteps)
    }

    /**
     * @LooperMode - Needed LEGACY to invoke runOnMainThread immediately.
     * It is valid behaviour, that `runOnMainThread` is scheduled on next UI thread run, but is non-compatible
     * with `waitForIt` because of locking mechanism and test runs in UI thread by default
     */
    @Test
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `switch to background thread from main thread`() {
        val ranSteps = mutableListOf<Int>()
        waitForIt(500) {
            ranSteps.add(0)
            runOnMainThread {
                ranSteps.add(1)
                ensureOnBackgroundThread {
                    Thread.sleep(100)
                    ranSteps.add(4)
                    Thread.sleep(100)
                    ranSteps.add(5)
                    it()
                }
                ranSteps.add(2)
            }
            ranSteps.add(3)
        }
        ranSteps.add(6)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), ranSteps)
    }

    @Test
    fun `invoke cancellable background task successfully`() {
        val ranSteps = mutableListOf<Int>()
        waitForIt(100) { done ->
            ranSteps.add(0)
            runOnBackgroundThread(
                delayMillis = 10,
                timeoutMillis = 50,
                block = {
                    ranSteps.add(2)
                    done()
                },
                onTimeout = {
                    // should not be called
                    ranSteps.add(4)
                }
            )
            ranSteps.add(1)
        }
        ranSteps.add(3)
        // wait for timeout
        Thread.sleep(100)
        assertEquals(listOf(0, 1, 2, 3), ranSteps)
    }

    @Test
    fun `invoke timeout on background task`() {
        val ranSteps = mutableListOf<Int>()
        waitForIt(100) { done ->
            ranSteps.add(0)
            runOnBackgroundThread(
                delayMillis = 10,
                timeoutMillis = 50,
                block = {
                    delay(60)
                    // should not be called
                    ranSteps.add(2)
                },
                onTimeout = {
                    // should not be called
                    ranSteps.add(4)
                    done()
                }
            )
            ranSteps.add(1)
        }
        ranSteps.add(3)
        // wait for timeout
        Thread.sleep(100)
        assertEquals(listOf(0, 1, 4, 3), ranSteps)
    }

    @Test
    fun `should block access within 2 threads`() {
        val ranSteps = mutableListOf<Int>()
        val locker = ThreadSafeAccess()
        waitForIt(3000) { done ->
            // first thread, should start as first
            runOnBackgroundThread(10) {
                locker.waitForAccess {
                    // simulate work but keep lock
                    Thread.sleep(2000)
                    ranSteps.add(0)
                }
            }
            // second thread, should wait for first
            runOnBackgroundThread(20) {
                locker.waitForAccess {
                    ranSteps.add(1)
                    done()
                }
            }
        }
        assertEquals(listOf(0, 1), ranSteps)
    }

    @Test
    fun `should not block access within same thread`() {
        val ranSteps = mutableListOf<Int>()
        val locker = ThreadSafeAccess()
        waitForIt(3000) { done ->
            runOnBackgroundThread(10) {
                locker.waitForAccess {
                    locker.waitForAccess {
                        locker.waitForAccess {
                            ranSteps.add(0)
                            done()
                        }
                    }
                }
            }
        }
        assertEquals(listOf(0), ranSteps)
    }

    @Test
    fun `should create singleton thread safe access and block access within 2 threads`() {
        val ranSteps = mutableListOf<Int>()
        val mutualLockName = "test"
        waitForIt(3000) { done ->
            // first thread, should start as first
            runOnBackgroundThread(10) {
                waitForAccess(mutualLockName) {
                    // simulate work but keep lock
                    Thread.sleep(2000)
                    ranSteps.add(0)
                }
            }
            // second thread, should wait for first
            runOnBackgroundThread(20) {
                waitForAccess(mutualLockName) {
                    ranSteps.add(1)
                    done()
                }
            }
        }
        assertEquals(listOf(0, 1), ranSteps)
    }

    @Test
    fun `should create singleton thread safe access and not block access within same thread`() {
        val ranSteps = mutableListOf<Int>()
        val mutualLockName = "test"
        waitForIt(3000) { done ->
            runOnBackgroundThread(10) {
                waitForAccess(mutualLockName) {
                    waitForAccess(mutualLockName) {
                        waitForAccess(mutualLockName) {
                            ranSteps.add(0)
                            done()
                        }
                    }
                }
            }
        }
        assertEquals(listOf(0), ranSteps)
    }

    @Test
    fun `should clear singleton thread safe access after usage`() {
        val ranSteps = mutableListOf<Int>()
        val mutualLockName = "test"
        assertFalse(ThreadSafeAccess.Companion.hasLock(mutualLockName))
        waitForIt(3000) { done ->
            // first thread, should start as first
            runOnBackgroundThread(10) {
                waitForAccess(mutualLockName) {
                    assertTrue(ThreadSafeAccess.Companion.hasLock(mutualLockName))
                    // simulate work but keep lock
                    Thread.sleep(2000)
                    ranSteps.add(0)
                }
            }
            // second thread, should wait for first
            runOnBackgroundThread(20) {
                waitForAccess(mutualLockName) {
                    assertTrue(ThreadSafeAccess.Companion.hasLock(mutualLockName))
                    ranSteps.add(1)
                }
                done()
            }
        }
        assertFalse(ThreadSafeAccess.Companion.hasLock(mutualLockName))
        assertEquals(listOf(0, 1), ranSteps)
    }

    @Test
    fun `should not block access within 2 threads for different lock names`() {
        val ranSteps = mutableListOf<Int>()
        waitForIt(3000) { done ->
            // first thread
            runOnBackgroundThread(10) {
                waitForAccess("thread1") {
                    // simulate work
                    Thread.sleep(2000)
                    ranSteps.add(0)
                    done()
                }
            }
            // second thread
            runOnBackgroundThread(20) {
                waitForAccess("thread2") {
                    ranSteps.add(1)
                }
            }
        }
        assertEquals(listOf(1, 0), ranSteps)
    }
}
