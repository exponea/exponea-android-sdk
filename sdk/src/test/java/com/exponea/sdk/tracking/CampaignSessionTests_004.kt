package com.exponea.sdk.tracking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.FlushMode.MANUAL
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.CampaignRepositoryImpl
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.util.ExponeaGson
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class CampaignSessionTests_004 : CampaignSessionTests_Base() {

    /**
     * Cold start, campaign click Start, SDK init in Resume
     */
    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun testBehavior_004() {
        val applicationContext = InstrumentationRegistry.getInstrumentation().context
        ExponeaConfigRepository.set(applicationContext, configuration)
        val campaignIntent = createDeeplinkIntent()
        val controller = Robolectric.buildActivity(TestActivity::class.java, campaignIntent)
        Exponea.flushMode = MANUAL
        controller.create()
        controller.start()
        assertFalse(Exponea.isInitialized)
        val preferences = ExponeaPreferencesImpl(applicationContext)
        val campaignRepository = CampaignRepositoryImpl(ExponeaGson.instance, preferences)
        val eventRepository = EventRepositoryImpl(applicationContext, preferences)
        val campaignEvent = campaignRepository.get()
        assertNotNull(campaignEvent)
        assertTrue(eventRepository.all().any { it.type == Constants.EventTypes.push })

        controller.resume()
        assertTrue(Exponea.isInitialized)
        controller.pause()
        controller.stop()
        controller.destroy()

        assertNull(Exponea.componentForTesting.campaignRepository.get())
        assertEquals(1, Exponea.componentForTesting.eventRepository.all().count {
            it.type == Constants.EventTypes.sessionStart
        }, "Only single session_start has to exists")
        val sessionEvent = Exponea.componentForTesting.eventRepository.all().find {
            it.type == Constants.EventTypes.sessionStart
        }
        assertNotNull(sessionEvent)
        assertNotNull(sessionEvent.properties)
        assertEquals(campaignEvent.completeUrl, sessionEvent.properties!!["location"])
        assertEquals(campaignEvent.source, sessionEvent.properties!!["utm_source"])
        assertEquals(campaignEvent.campaign, sessionEvent.properties!!["utm_campaign"])
        assertEquals(campaignEvent.content, sessionEvent.properties!!["utm_content"])
        assertEquals(campaignEvent.term, sessionEvent.properties!!["utm_term"])
    }

    /**
     * Used by test testBehavior_004 (Cold start, campaign click Start, SDK init in Resume)
     */
    class TestActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_AppCompat)
            Exponea.handleCampaignIntent(intent, applicationContext)
        }

        override fun onResume() {
            super.onResume()
            initExponea(context = this)
        }
    }
}
