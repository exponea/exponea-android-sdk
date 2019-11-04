package com.exponea.sdk.tracking

import android.app.Activity
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CampaignSessionTests_003 : CampaignSessionTests_Base() {

    /**
     * Hot Start with Resumed Session, Campaign Click Start, SDK init before onResume
     */
    @Test
    fun testBehavior_003() {
        // first run will initialize SDK
        val firstRun = Robolectric.buildActivity(TestActivity::class.java)
        firstRun.create(Bundle.EMPTY)
        firstRun.resume()
        var sessionStartRecord = Exponea.component.eventRepository.all().last {
            it.item.type == Constants.EventTypes.sessionStart
        }
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

        secondRun.resume()  // session is resumed, so no campaign cache clear is done
        secondRun.pause()

        assertNull(Exponea.component.campaignRepository.get())
        val sessionEvent = Exponea.component.eventRepository.all().findLast {
            it.item.type == Constants.EventTypes.sessionStart
        }?.item
        assertNotNull(sessionEvent)
        assertNotNull(sessionEvent.properties)
        assertNull(sessionEvent.properties!!["location"])
        assertNull(sessionEvent.properties!!["utm_source"])
        assertNull(sessionEvent.properties!!["utm_campaign"])
        assertNull(sessionEvent.properties!!["utm_content"])
        assertNull(sessionEvent.properties!!["utm_term"])

        val hasAnySessionEnd = Exponea.component.eventRepository.all().any {
            it.item.type == Constants.EventTypes.sessionEnd
        }
        assertFalse(hasAnySessionEnd)
        val sessionStartEvents = Exponea.component.eventRepository.all().filter {
            it.item.type == Constants.EventTypes.sessionStart
        }
        assertEquals(1, sessionStartEvents.size)
        assertEquals(sessionStartRecord.id, sessionStartEvents.first().id)
    }

    /**
     * Used by test testBahavior_003 (Hot Start with Resumed Session, Campaign Click Start, SDK init before onResume)
     */
    class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            initExponea(applicationContext)
            Exponea.handleCampaignIntent(intent, applicationContext)
        }
    }
}
