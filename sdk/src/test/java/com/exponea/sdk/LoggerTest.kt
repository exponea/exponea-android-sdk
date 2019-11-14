package com.exponea.sdk

import android.util.Log
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.Logger
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LoggerTest : ExponeaSDKTest() {

    // Log level OFF
    @Test
    fun setLoggerLevelToOff_shouldBeEqual5() {
        Logger.level = Logger.Level.OFF
        assertEquals(5, Logger.level.value)
    }
    @Test
    fun setLoggerLevelToOff_shouldBeEqualOff() {
        Logger.level = Logger.Level.OFF
        assertEquals(Logger.Level.OFF, Logger.level)
    }

    // Log level ERROR
    @Test
    fun setLoggerLevelToError_shouldBeEqual4() {
        Logger.level = Logger.Level.ERROR
        assertEquals(4, Logger.level.value)
    }
    @Test
    fun setLoggerLevelToError_shouldBeEqualError() {
        Logger.level = Logger.Level.ERROR
        assertEquals(Logger.Level.ERROR, Logger.level)
    }

    // Log level WARN
    @Test
    fun setLoggerLevelToWarning_shouldBeEqual3() {
        Logger.level = Logger.Level.WARN
        assertEquals(3, Logger.level.value)
    }
    @Test
    fun setLoggerLevelToWarning_shouldBeEqualWarn() {
        Logger.level = Logger.Level.WARN
        assertEquals(Logger.Level.WARN, Logger.level)
    }

    // Log level INFO
    @Test
    fun setLoggerLevelToInfo_shouldBeEqual2() {
        Logger.level = Logger.Level.INFO
        assertEquals(2, Logger.level.value)
    }
    @Test
    fun setLoggerLevelToInfo_shouldBeEqualInfo() {
        Logger.level = Logger.Level.INFO
        assertEquals(Logger.Level.INFO, Logger.level)
    }

    // Log level DEBUG
    @Test
    fun setLoggerLevelToDebug_shouldBeLessThan1() {
        Logger.level = Logger.Level.DEBUG
        assertEquals(1, Logger.level.value)
    }
    @Test
    fun setLoggerLevelToDebug_shouldBeEqualDebug() {
        Logger.level = Logger.Level.DEBUG
        assertEquals(Logger.Level.DEBUG, Logger.level)
    }

    // Log level VERBOSE
    @Test
    fun setLoggerLevelToVerbose_shouldBeEqual1() {
        Logger.level = Logger.Level.VERBOSE
        assertEquals(0, Logger.level.value)
    }
    @Test
    fun setLoggerLevelToVerbose_shouldBeEqualVerbose() {
        Logger.level = Logger.Level.VERBOSE
        assertEquals(Logger.Level.VERBOSE, Logger.level)
    }

    @Before
    fun before() {
        mockkStatic(Log::class)
    }

    @After
    fun after() {
        unmockkAll()
    }

    private fun logMockMessageOnAllLevels() {
        Logger.e(this, "mock message")
        Logger.w(this, "mock message")
        Logger.i(this, "mock message")
        Logger.d(this, "mock message")
        Logger.v(this, "mock message")
    }

    @Test
    fun `logging on verbose level`() {
        Logger.level = Logger.Level.VERBOSE
        logMockMessageOnAllLevels()
        verify(exactly = 1) {
            Log.e("LoggerTest", "mock message")
            Log.w("LoggerTest", "mock message")
            Log.i("LoggerTest", "mock message")
            Log.d("LoggerTest", "mock message")
            Log.v("LoggerTest", "mock message")
        }
    }

    @Test
    fun `logging on off level`() {
        Logger.level = Logger.Level.OFF
        logMockMessageOnAllLevels()
        verify(exactly = 0) {
            Log.e("LoggerTest", "mock message")
            Log.w("LoggerTest", "mock message")
            Log.i("LoggerTest", "mock message")
            Log.d("LoggerTest", "mock message")
            Log.v("LoggerTest", "mock message")
        }
    }

    @Test
    fun `logging on error level`() {
        Logger.level = Logger.Level.ERROR
        logMockMessageOnAllLevels()
        verify(exactly = 1) {
            Log.e("LoggerTest", "mock message")
        }
        verify(exactly = 0) {
            Log.w("LoggerTest", "mock message")
            Log.i("LoggerTest", "mock message")
            Log.d("LoggerTest", "mock message")
            Log.v("LoggerTest", "mock message")
        }
    }
}
