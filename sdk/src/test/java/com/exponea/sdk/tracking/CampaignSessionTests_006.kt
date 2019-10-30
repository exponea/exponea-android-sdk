package com.exponea.sdk.tracking

import android.app.Activity
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class CampaignSessionTests_006 : CampaignSessionTests_Base() {

    /**
     * Hot Start with Resumed Session, Campaign click start, SDK init in onResume
     */
    @Test
    fun testBehavior_006() {
        // first run will initialize SDK
        val firstRun = Robolectric.buildActivity(TestActivity::class.java)
        firstRun.create(Bundle.EMPTY)
        firstRun.resume()
        firstRun.pause()
        firstRun.destroy()

        // second run will handle Campaign Intent, but session will be resumed
        val campaignIntent = createDeeplinkIntent()
        val secondRun = Robolectric.buildActivity(TestActivity::class.java, campaignIntent)
        secondRun.create()

        assertTrue(Exponea.isInitialized)
        val campaignEvent = Exponea.component.campaignRepository.get()
        assertNotNull(campaignEvent)
        assertTrue(Exponea.component.eventRepository.all().any { it.item.type == Constants.EventTypes.push })

        secondRun.resume()

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

        val hasAnySessionEnd = Exponea.component.eventRepository.all().any {
            it.item.type == Constants.EventTypes.sessionEnd
        }
        assertFalse(hasAnySessionEnd)
        val sessionStartEvents = Exponea.component.eventRepository.all().filter {
            it.item.type == Constants.EventTypes.sessionStart
        }
        assertEquals(1, sessionStartEvents.size)
    }

    /**
     * Used by test testBehavior_006 (Hot Start with Resumed Session, Campaign click start, SDK init in onResume)
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
