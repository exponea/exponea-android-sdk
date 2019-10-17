package com.exponea.sdk.runcatching

import android.util.Log
import com.exponea.sdk.Exponea
import com.exponea.sdk.util.logOnException
import com.exponea.sdk.util.returnOnException
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
internal class ExceptionHandlingTest {

    private val LOGTAG_TO_WATCH = Exponea.javaClass.simpleName

    private class TestPurposeException : RuntimeException("Exception to test purposes")

    private var errorLogCount: Int = 0

    @Before
    fun prepareTest() {
        errorLogCount = 0
        mockkStatic(Log::class)
        every { Log.e(LOGTAG_TO_WATCH, any()) } answers {
            errorLogCount++
        }
        every { Log.e(LOGTAG_TO_WATCH, any(), any()) } answers {
            errorLogCount++
        }

    }

    @After
    fun release() {
        unmockkStatic(Log::class)
    }

    @Test
    fun logOnExceptionWithoutExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        runCatching {}.logOnException()
        assertEquals(0, errorLogCount)
    }

    @Test
    fun logOnExceptionWithoutExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        runCatching {}.logOnException()
        assertEquals(0, errorLogCount)
    }

    @Test(expected = TestPurposeException::class)
    fun logOnExceptionWithExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        runCatching {
            throw TestPurposeException()
        }.logOnException()
        assertEquals(1, errorLogCount)
    }

    @Test
    fun logOnExceptionWithExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        runCatching {
            throw TestPurposeException()
        }.logOnException()
        assertEquals(1, errorLogCount)
    }

    @Test
    fun logOnExceptionWithLoggerExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        every { Log.e(LOGTAG_TO_WATCH, any()) } throws TestPurposeException()
        runCatching {}.logOnException()
        assertEquals(0, errorLogCount)
    }

    @Test
    fun logOnExceptionWithLoggerExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        every { Log.e(LOGTAG_TO_WATCH, any()) } throws TestPurposeException()
        runCatching {}.logOnException()
        assertEquals(0, errorLogCount)
    }

    @Test
    fun returnOnExceptionWithoutExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        runCatching {}.returnOnException { fail("mapThrowable should not be called") }
        assertEquals(0, errorLogCount)
    }

    @Test
    fun returnOnExceptionWithoutExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        runCatching {}.returnOnException { fail("mapThrowable should not be called") }
        assertEquals(0, errorLogCount)
    }

    @Test(expected = TestPurposeException::class)
    fun returnOnExceptionWithExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        runCatching {
            throw TestPurposeException()
        }.returnOnException {
            fail("should not be called")
        }
        assertEquals(1, errorLogCount)
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
    }

    @Test
    fun returnOnExceptionWithLoggerExceptionNotInSafeMode() {
        Exponea.safeModeEnabled = false
        every { Log.e(LOGTAG_TO_WATCH, any()) } throws TestPurposeException()
        runCatching {}.returnOnException { fail("mapThrowable should not be called") }
        assertEquals(0, errorLogCount)
    }

    @Test
    fun returnOnExceptionWithLoggerExceptionInSafeMode() {
        Exponea.safeModeEnabled = true
        every { Log.e(LOGTAG_TO_WATCH, any()) } throws TestPurposeException()
        runCatching {}.returnOnException { fail("mapThrowable should not be called") }
        assertEquals(0, errorLogCount)
    }
}