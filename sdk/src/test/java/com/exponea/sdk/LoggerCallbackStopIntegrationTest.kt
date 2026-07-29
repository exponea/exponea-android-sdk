package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.FlushFinishedCallback
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.LoggerCallback
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.Logger
import io.mockk.every
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val MOCK_MESSAGE = "mock message"

@RunWith(RobolectricTestRunner::class)
internal class LoggerCallbackStopIntegrationTest : ExponeaSDKTest() {

    private class RecordingCallback : LoggerCallback {
        val entries = mutableListOf<String>()
        override fun onLog(level: Logger.Level, message: String, throwable: Throwable?) {
            entries.add(message)
        }
    }

    @Test
    fun `callback keeps receiving logs after stopIntegration`() {
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(integrationConfig = StreamConfig(streamId = "stream-id"))
        )
        every { anyConstructed<FlushManagerImpl>().flushData(any()) } answers {
            firstArg<FlushFinishedCallback?>()?.invoke(Result.success(Unit))
        }
        val callback = RecordingCallback()
        Exponea.registerLoggerCallback(callback)

        try {
            waitForIt {
                Exponea.stopIntegration {
                    it()
                }
            }
            assertFalse(Exponea.isInitialized)

            callback.entries.clear()
            Logger.i(this, MOCK_MESSAGE)

            assertEquals(listOf(MOCK_MESSAGE), callback.entries)
        } finally {
            Exponea.unregisterLoggerCallback(callback)
        }
    }
}
