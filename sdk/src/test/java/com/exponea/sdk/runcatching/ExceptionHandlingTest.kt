package com.exponea.sdk.runcatching

import android.util.Log
import com.exponea.sdk.Exponea
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.returnOnException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExceptionHandlingTest : ExponeaSDKTest() {

    private val LOGTAG_TO_WATCH = Exponea.javaClass.simpleName

    internal class TestPurposeException : RuntimeException("Exception to test purposes")

    private var errorLogCount: Int = 0

    @Before
    fun prepareTest() {
        errorLogCount = 0
        mockkStatic(Log::class)
        Exponea.telemetry = mockk() {
            every { reportCaughtException(any()) } just Runs
            every { reportLog(any(), any()) } just Runs
        }
        every { Log.e(LOGTAG_TO_WATCH, any()) } answers {
            errorLogCount++
        }
        every { Log.e(LOGTAG_TO_WATCH, any(), any()) } answers {
            errorLogCount++
        }
    }

    @Test
    fun logOnExceptionWithoutExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        runCatching {}.logOnException()
        assertEquals(0, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test
    fun logOnExceptionWithoutExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        runCatching {}.logOnException()
        assertEquals(0, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test(expected = TestPurposeException::class)
    fun logOnExceptionWithExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        runCatching {
            throw TestPurposeException()
        }.logOnException()
        assertEquals(1, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test
    fun logOnExceptionWithExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        runCatching {
            throw TestPurposeException()
        }.logOnException()
        assertEquals(1, errorLogCount)
        verify(exactly = 1) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test
    fun logOnExceptionWithLoggerExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        every { Log.e(LOGTAG_TO_WATCH, any()) } throws TestPurposeException()
        runCatching {}.logOnException()
        assertEquals(0, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test
    fun logOnExceptionWithLoggerExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        every { Log.e(LOGTAG_TO_WATCH, any()) } throws TestPurposeException()
        runCatching {}.logOnException()
        assertEquals(0, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test
    fun returnOnExceptionWithoutExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        runCatching {}.returnOnException { fail("mapThrowable should not be called") }
        assertEquals(0, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test
    fun returnOnExceptionWithoutExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        runCatching {}.returnOnException { fail("mapThrowable should not be called") }
        assertEquals(0, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test(expected = TestPurposeException::class)
    fun returnOnExceptionWithExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        runCatching {
            throw TestPurposeException()
        }.returnOnException {
            fail("should not be called")
        }
    }

    @Test
    fun returnOnExceptionWithExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        var mapThrowableCalled = false
        runCatching {
            throw TestPurposeException()
        }.returnOnException {
            mapThrowableCalled = true
            assertTrue(it is TestPurposeException)
        }
        assertTrue(mapThrowableCalled)
        assertEquals(1, errorLogCount)
        verify(exactly = 1) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test
    fun returnOnExceptionWithLoggerExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        every { Log.e(LOGTAG_TO_WATCH, any()) } throws TestPurposeException()
        runCatching {}.returnOnException { fail("mapThrowable should not be called") }
        assertEquals(0, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }

    @Test
    fun returnOnExceptionWithLoggerExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        every { Log.e(LOGTAG_TO_WATCH, any()) } throws TestPurposeException()
        runCatching {}.returnOnException { fail("mapThrowable should not be called") }
        assertEquals(0, errorLogCount)
        verify(exactly = 0) { Exponea.telemetry?.reportCaughtException(any()) }
    }
}
