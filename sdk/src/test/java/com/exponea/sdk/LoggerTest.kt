package com.exponea.sdk

import com.exponea.sdk.util.Logger
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggerTest {

    // Log level OFF
    @Test
    fun setLoggerLevelToOff_shouldBeEqual5() {
        Logger.level = Logger.Level.OFF
        assertEquals(Logger.level.value, 5)
    }
    @Test
    fun setLoggerLevelToOff_shouldBeEqualOff() {
        Logger.level = Logger.Level.OFF
        assertEquals(Logger.level, Logger.Level.OFF)
    }

    // Log level ERROR
    @Test
    fun setLoggerLevelToError_shouldBeEqual4() {
        Logger.level = Logger.Level.ERROR
        assertEquals(Logger.level.value, 4)
    }
    @Test
    fun setLoggerLevelToError_shouldBeEqualError() {
        Logger.level = Logger.Level.ERROR
        assertEquals(Logger.level, Logger.Level.ERROR)
    }

    // Log level WARN
    @Test
    fun setLoggerLevelToWarning_shouldBeEqual3() {
        Logger.level = Logger.Level.WARN
        assertEquals(Logger.level.value, 3)
    }
    @Test
    fun setLoggerLevelToWarning_shouldBeEqualWarn() {
        Logger.level = Logger.Level.WARN
        assertEquals(Logger.level, Logger.Level.WARN)
    }

    // Log level INFO
    @Test
    fun setLoggerLevelToInfo_shouldBeEqual2() {
        Logger.level = Logger.Level.INFO
        assertEquals(Logger.level.value, 2)
    }
    @Test
    fun setLoggerLevelToInfo_shouldBeEqualInfo() {
        Logger.level = Logger.Level.INFO
        assertEquals(Logger.level, Logger.Level.INFO)
    }

    // Log level VERBOSE
    @Test
    fun setLoggerLevelToVerbose_shouldBeEqual1() {
        Logger.level = Logger.Level.VERBOSE
        assertEquals(Logger.level.value, 1)
    }
    @Test
    fun setLoggerLevelToVerbose_shouldBeEqualVerbose() {
        Logger.level = Logger.Level.VERBOSE
        assertEquals(Logger.level, Logger.Level.VERBOSE)
    }

    // Log level DEBUG
    @Test
    fun setLoggerLevelToDebug_shouldBeLessThan1() {
        Logger.level = Logger.Level.DEBUG
        assertTrue { Logger.level.value < 1 }
    }
    @Test
    fun setLoggerLevelToDebug_shouldBeEqualDebug() {
        Logger.level = Logger.Level.DEBUG
        assertEquals(Logger.level, Logger.Level.DEBUG)
    }
}