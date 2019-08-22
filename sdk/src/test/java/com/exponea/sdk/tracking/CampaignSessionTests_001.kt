package com.exponea.sdk.tracking

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CampaignSessionTests_001 : CampaignSessionTests_Base() {

    /**
     * Cold start, Campaign Click Start, SDK init before onResume
     */
    @Test
    fun testBehavior_001() {
        val campaignIntent = createDeeplinkIntent()
        val controller = Robolectric.buildActivity(TestActivity::class.java, campaignIntent)
        controller.create()

        assertTrue(Exponea.isInitialized)
        val campaignEvent = Exponea.component.campaignRepository.get()
        assertNotNull(campaignEvent)
        assertTrue(Exponea.component.eventRepository.all().any { it.item.type == Constants.EventTypes.push })

        controller.resume()

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
    }

    private fun createDeeplinkIntent() = Intent().apply {
        this.action = Intent.ACTION_VIEW
        this.addCategory(Intent.CATEGORY_DEFAULT)
        this.addCategory(Intent.CATEGORY_BROWSABLE)
        this.data = Uri.parse(CAMPAIGN_URL)
    }

    /**
     * Used by test testBehavior_001 (Cold start, Campaign Click Start, SDK init before onResume)
     */
    class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            initExponea(applicationContext)
            Exponea.handleCampaignIntent(intent, applicationContext)
        }
    }

}