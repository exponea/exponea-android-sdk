package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.testutil.ExponeaSDKTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
internal class TokenTrackingFrequencyTest : ExponeaSDKTest() {

    private fun setupConfiguration(configuration: ExponeaConfiguration) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.init(context, configuration)
        waitUntilFlushed()
    }

    @Test
    fun dailyToken() {
        val config = ExponeaConfiguration()
        config.tokenTrackFrequency = ExponeaConfiguration.TokenFrequency.DAILY
        setupConfiguration(config)
        assertEquals(Exponea.tokenTrackFrequency, ExponeaConfiguration.TokenFrequency.DAILY)
    }

    @Test
    fun everyLaunchToken() {
        val config = ExponeaConfiguration()
        config.tokenTrackFrequency = ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH
        setupConfiguration(config)
        assertEquals(Exponea.tokenTrackFrequency, ExponeaConfiguration.TokenFrequency.EVERY_LAUNCH)
    }

    @Test
    fun onTokenChangeToken() {
        val config = ExponeaConfiguration()
        config.tokenTrackFrequency = ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE
        setupConfiguration(config)
        assertEquals(Exponea.tokenTrackFrequency, ExponeaConfiguration.TokenFrequency.ON_TOKEN_CHANGE)
    }
}
