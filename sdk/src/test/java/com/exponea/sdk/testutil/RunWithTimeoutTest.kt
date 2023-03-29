package com.exponea.sdk.testutil

import android.os.Build.VERSION_CODES.M
import com.exponea.sdk.util.runWithTimeout
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class RunWithTimeoutTest {

    @Test
    fun `should complete work`() {
        var workDone = false
        runWithTimeout(1000, {
            workDone = true
        }, {
            fail("OnExpire should not be called")
        })
        assertTrue { workDone }
    }

    @Test
    fun `should complete work for result`() {
        val workDone = runWithTimeout(1000, {
            true
        }, {
            fail("OnExpire should not be called")
        })
        assertTrue { workDone }
    }

    @Test
    fun `should cancel incomplete work`() {
        val workResult = runWithTimeout(1000, {
            Thread.sleep(2000)
            "WORK DONE"
        }, {
            "WORK TIMEOUT"
        })
        Thread.sleep(3000)
        assertEquals("WORK TIMEOUT", workResult)
    }

    @Config(sdk = [M])
    @Test
    fun `should complete work - pre API 24`() {
        var workDone = false
        runWithTimeout(1000, {
            workDone = true
        }, {
            fail("OnExpire should not be called")
        })
        assertTrue { workDone }
    }

    @Config(sdk = [M])
    @Test
    fun `should complete work for result - pre API 24`() {
        val workDone = runWithTimeout(1000, {
            true
        }, {
            fail("OnExpire should not be called")
        })
        assertTrue { workDone }
    }

    @Config(sdk = [M])
    @Test
    fun `should cancel incomplete work - pre API 24`() {
        val workResult = runWithTimeout(1000, {
            Thread.sleep(2000)
            "WORK DONE"
        }, {
            "WORK TIMEOUT"
        })
        Thread.sleep(3000)
        assertEquals("WORK TIMEOUT", workResult)
    }
}
