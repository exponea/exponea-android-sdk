package com.exponea.sdk

import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.Logger
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

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
}
