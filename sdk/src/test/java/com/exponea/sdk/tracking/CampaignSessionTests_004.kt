package com.exponea.sdk.tracking

import android.app.Activity
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.repository.ExponeaConfigRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CampaignSessionTests_004 : CampaignSessionTests_Base() {

    /**
     * Cold start, campaign click Start, SDK init in Resume
     */
    @Test
    fun testBehavior_004() {
        ExponeaConfigRepository.set(InstrumentationRegistry.getInstrumentation().context, configuration)
        val campaignIntent = createDeeplinkIntent()
        val controller = Robolectric.buildActivity(TestActivity::class.java, campaignIntent)
        controller.create()

        assertTrue(Exponea.isInitialized)
        val campaignEvent = Exponea.component.campaignRepository.get()
        assertNotNull(campaignEvent)
        assertTrue(Exponea.component.eventRepository.all().any { it.item.type == Constants.EventTypes.push })

        controller.resume()
        assertTrue(Exponea.isInitialized)
        assertNull(Exponea.component.campaignRepository.get())
        val sessionEvent = Exponea.component.eventRepository.all().find {
            it.item.type == Constants.EventTypes.sessionStart
        }?.item
        assertNotNull(sessionEvent)
        assertNotNull(sessionEvent.properties)
        assertEquals(campaignEvent.completeUrl, sessionEvent.properties!!["location"])
        assertEquals(campaignEvent.source, sessionEvent.properties!!["utm_source"])
        assertEquals(campaignEvent.campaign, sessionEvent.properties!!["utm_campaign"])
        assertEquals(campaignEvent.content, sessionEvent.properties!!["utm_content"])
        assertEquals(campaignEvent.term, sessionEvent.properties!!["utm_term"])
        waitUntilFlushed()
    }

    /**
     * Used by test testBehavior_004 (Cold start, campaign click Start, SDK init in Resume)
     */
    class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            Exponea.handleCampaignIntent(intent, applicationContext)
        }

        override fun onResume() {
            super.onResume()
            initExponea(applicationContext)
        }
    }
}
