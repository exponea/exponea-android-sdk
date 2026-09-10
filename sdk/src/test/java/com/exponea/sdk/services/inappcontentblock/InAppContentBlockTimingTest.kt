package com.exponea.sdk.services.inappcontentblock

import com.exponea.sdk.Exponea
import com.exponea.sdk.testutil.RecordingLoggerCallback
import com.exponea.sdk.testutil.latestInAppContentBlockTimingLog
import com.exponea.sdk.testutil.parseInAppContentBlockTimingLog
import com.exponea.sdk.util.Logger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppContentBlockTimingTest {

    @Test
    fun `emits required fields with encoded optional values`() {
        val callback = RecordingLoggerCallback()
        val previousLevel = Logger.level
        Logger.level = Logger.Level.DEBUG
        Exponea.registerLoggerCallback(callback)
        try {
            InAppContentBlockTiming.log(
                contentBlockId = "id=1",
                source = InAppContentBlockTiming.RequestSource.NORMALIZATION_FINISHED,
                contentBlockName = "Name, one",
                normalizationMode = InAppContentBlockTiming.NormalizationMode.CACHE_MISS,
                properties = mapOf("result" to "valid")
            )

            val (level, message) = latestInAppContentBlockTimingLog(callback)
            val fields = parseInAppContentBlockTimingLog(message)
            assertEquals(Logger.Level.DEBUG, level)
            assertEquals("id%3D1", fields["contentBlockId"])
            assertTrue(fields["timestampMs"]?.toLongOrNull() ?: -1L >= 0)
            assertEquals("normalization_finished", fields["source"])
            assertEquals("Name%2C+one", fields["contentBlockName"])
            assertEquals("cache_miss", fields["normalizationMode"])
            assertEquals("valid", fields["result"])
        } finally {
            Exponea.unregisterLoggerCallback(callback)
            Logger.level = previousLevel
        }
    }

    @Test
    fun `does not build timing log when debug logging is disabled`() {
        val callback = RecordingLoggerCallback()
        val previousLevel = Logger.level
        Logger.level = Logger.Level.INFO
        Exponea.registerLoggerCallback(callback)
        try {
            InAppContentBlockTiming.log(
                contentBlockId = "id",
                source = InAppContentBlockTiming.RequestSource.CONTENT_LOADED,
                properties = mapOf(
                    "value" to object {
                        override fun toString(): String = error("Timing properties must not be evaluated")
                    }
                )
            )

            assertTrue(callback.logs.none { it.second.startsWith("InAppCB timing: ") })
        } finally {
            Exponea.unregisterLoggerCallback(callback)
            Logger.level = previousLevel
        }
    }
}
