package com.exponea.sdk

import android.util.Log
import com.exponea.sdk.util.Logger
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.never
import org.mockito.kotlin.times

private const val MOCK_MESSAGE = "mock message"
private const val LOGGER_TEST_TAG = "LoggerTest"

internal class LoggerTest {
    private lateinit var logStatic: MockedStatic<Log>

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
        logStatic = mockStatic(Log::class.java)
    }

    @After
    fun tearDown() {
        logStatic.close()
    }

    private fun logMockMessageOnAllLevels() {
        Logger.e(this, MOCK_MESSAGE)
        Logger.w(this, MOCK_MESSAGE)
        Logger.i(this, MOCK_MESSAGE)
        Logger.d(this, MOCK_MESSAGE)
        Logger.v(this, MOCK_MESSAGE)
    }

    @Test
    fun `logging on verbose level`() {
        Logger.level = Logger.Level.VERBOSE
        logMockMessageOnAllLevels()

        logStatic.verify(
            {
                Log.e(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.w(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.i(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.d(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.v(LOGGER_TEST_TAG, MOCK_MESSAGE)
            },
            times(1)
        )
    }

    @Test
    fun `logging on off level`() {
        Logger.level = Logger.Level.OFF
        logMockMessageOnAllLevels()

        logStatic.verify(
            {
                Log.e(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.w(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.i(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.d(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.v(LOGGER_TEST_TAG, MOCK_MESSAGE)
            },
            never()
        )
    }

    @Test
    fun `logging on error level`() {
        Logger.level = Logger.Level.ERROR
        logMockMessageOnAllLevels()
        logStatic.verify(
            { Log.e(LOGGER_TEST_TAG, MOCK_MESSAGE) },
            times(1)
        )
        logStatic.verify(
            {
                Log.w(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.i(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.d(LOGGER_TEST_TAG, MOCK_MESSAGE)
                Log.v(LOGGER_TEST_TAG, MOCK_MESSAGE)
            },
            never()
        )
    }
}
