package com.exponea.sdk.tracking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.models.Constants
import com.exponea.sdk.testutil.componentForTesting
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CampaignSessionTests_008 : CampaignSessionTests_Base() {

    /**
     * Hot start with new session, Campaign Click Start, SDK init after onResume
     */
    @Test
    fun testBehavior_008() {
        initExponea(InstrumentationRegistry.getInstrumentation().context)
        val campaignIntent = createDeeplinkIntent()
        val controller = Robolectric.buildActivity(TestActivity::class.java, campaignIntent)
        controller.create()

        assertTrue(Exponea.isInitialized)
        val campaignEvent = Exponea.componentForTesting.campaignRepository.get()
        assertNotNull(campaignEvent)
        assertTrue(Exponea.componentForTesting.eventRepository.all().any { it.type == Constants.EventTypes.push })

        controller.resume()
        assertTrue(Exponea.isInitialized)
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
     * Used by test testBehavior_008 (Hot start with new session, Campaign Click Start, SDK init after onResume)
     */
    class TestActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_AppCompat)
            Exponea.handleCampaignIntent(intent, applicationContext)
        }

        override fun onPostResume() {
            super.onPostResume()
            initExponea(applicationContext)
        }
    }
}
