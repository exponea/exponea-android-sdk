package com.exponea.sdk.util

import com.exponea.sdk.testutil.waitForIt
import kotlin.test.assertEquals
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
}
