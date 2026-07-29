package com.exponea.sdk

import android.util.Log
import com.exponea.sdk.models.LoggerCallback
import com.exponea.sdk.util.Logger
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
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

    private class RecordingCallback : LoggerCallback {
        data class Entry(val level: Logger.Level, val message: String, val throwable: Throwable?)
        val entries = mutableListOf<Entry>()
        override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
            entries.add(Entry(level, message, throwable))
        }
    }

    @Test
    fun `callback receives all levels unconditionally even when logging is off`() {
        Logger.level = Logger.Level.OFF
        val callback = RecordingCallback()
        val errorMessage = "error message"
        val warnMessage = "warn message"
        val infoMessage = "info message"
        val debugMessage = "debug message"
        val verboseMessage = "verbose message"
        Exponea.registerLoggerCallback(callback)

        try {
            Logger.e(this, errorMessage)
            Logger.w(this, warnMessage)
            Logger.i(this, infoMessage)
            Logger.d(this, debugMessage)
            Logger.v(this, verboseMessage)
        } finally {
            Exponea.unregisterLoggerCallback(callback)
        }

        // Console output is gated by level (OFF), but callbacks fire regardless.
        logStatic.verify(
            {
                Log.e(LOGGER_TEST_TAG, errorMessage)
                Log.w(LOGGER_TEST_TAG, warnMessage)
                Log.i(LOGGER_TEST_TAG, infoMessage)
                Log.d(LOGGER_TEST_TAG, debugMessage)
                Log.v(LOGGER_TEST_TAG, verboseMessage)
            },
            never()
        )
        assertEquals(
            listOf(
                Logger.Level.ERROR to errorMessage,
                Logger.Level.WARN to warnMessage,
                Logger.Level.INFO to infoMessage,
                Logger.Level.DEBUG to debugMessage,
                Logger.Level.VERBOSE to verboseMessage
            ),
            callback.entries.map { it.level to it.message }
        )
    }

    @Test
    fun `callback receives throwable from error overload`() {
        Logger.level = Logger.Level.OFF
        val throwable = RuntimeException("boom")
        val callback = RecordingCallback()
        Exponea.registerLoggerCallback(callback)

        try {
            Logger.e(this, MOCK_MESSAGE, throwable)
        } finally {
            Exponea.unregisterLoggerCallback(callback)
        }

        assertEquals(1, callback.entries.size)
        assertEquals(Logger.Level.ERROR, callback.entries[0].level)
        assertSame(throwable, callback.entries[0].throwable)
    }

    @Test
    fun `error without throwable passes null`() {
        Logger.level = Logger.Level.OFF
        val callback = RecordingCallback()
        Exponea.registerLoggerCallback(callback)

        try {
            Logger.e(this, MOCK_MESSAGE)
        } finally {
            Exponea.unregisterLoggerCallback(callback)
        }

        assertEquals(1, callback.entries.size)
        assertNull(callback.entries[0].throwable)
    }

    @Test
    fun `unregistered callback stops receiving logs`() {
        Logger.level = Logger.Level.OFF
        val callback = RecordingCallback()
        Exponea.registerLoggerCallback(callback)
        Logger.i(this, MOCK_MESSAGE)
        Exponea.unregisterLoggerCallback(callback)
        Logger.i(this, MOCK_MESSAGE)

        assertEquals(1, callback.entries.size)
    }

    @Test
    fun `all registered callbacks are notified`() {
        Logger.level = Logger.Level.OFF
        val first = RecordingCallback()
        val second = RecordingCallback()
        Exponea.registerLoggerCallback(first)
        Exponea.registerLoggerCallback(second)

        try {
            Logger.i(this, MOCK_MESSAGE)
        } finally {
            Exponea.unregisterLoggerCallback(first)
            Exponea.unregisterLoggerCallback(second)
        }

        assertEquals(1, first.entries.size)
        assertEquals(1, second.entries.size)
    }

    @Test
    fun `throwing callback does not break other callbacks or caller`() {
        Logger.level = Logger.Level.OFF
        val throwing = object : LoggerCallback {
            override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
                throw RuntimeException("callback failure")
            }
        }
        val healthy = RecordingCallback()
        Exponea.registerLoggerCallback(throwing)
        Exponea.registerLoggerCallback(healthy)

        try {
            Logger.i(this, MOCK_MESSAGE)
        } finally {
            Exponea.unregisterLoggerCallback(throwing)
            Exponea.unregisterLoggerCallback(healthy)
        }

        // The throwing callback is isolated; the healthy one still receives the log.
        assertEquals(1, healthy.entries.size)
    }

    @Test
    fun `throwing callback failure is reported via android logger with LoggerCallback tag`() {
        Logger.level = Logger.Level.OFF
        val failure = RuntimeException("callback failure")
        val throwing = object : LoggerCallback {
            override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
                throw failure
            }
        }
        Exponea.registerLoggerCallback(throwing)

        try {
            Logger.i(this, MOCK_MESSAGE)
        } finally {
            Exponea.unregisterLoggerCallback(throwing)
        }

        logStatic.verify(
            { Log.e("LoggerCallback", "Logger callback failed", failure) },
            times(1)
        )
    }

    @Test
    fun `callback registered before SDK init still receives logs`() {
        Exponea.isInitialized = false
        Logger.level = Logger.Level.OFF
        val callback = RecordingCallback()
        Exponea.registerLoggerCallback(callback)

        try {
            Logger.i(this, MOCK_MESSAGE)
        } finally {
            Exponea.unregisterLoggerCallback(callback)
        }

        assertEquals(1, callback.entries.size)
    }

    @Test
    fun `registering the same callback instance multiple times only notifies it once per log`() {
        Logger.level = Logger.Level.OFF
        val callback = RecordingCallback()
        Exponea.registerLoggerCallback(callback)
        Exponea.registerLoggerCallback(callback)

        try {
            Logger.i(this, MOCK_MESSAGE)
        } finally {
            Exponea.unregisterLoggerCallback(callback)
        }

        assertEquals(1, callback.entries.size)
    }
}
