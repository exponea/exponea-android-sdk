package com.exponea.sdk.tracking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.models.Constants
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.CampaignRepositoryImpl
import com.exponea.sdk.repository.EventRepositoryImpl
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.runInSingleThread
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

@RunWith(RobolectricTestRunner::class)
internal class CampaignSessionTests_007 : CampaignSessionTests_Base() {

    /**
     * Cold start, Campaign click start, SDK init after onResume
     */
    @Test
    fun testBehavior_007() = runInSingleThread { idleThreads ->
        val applicationContext = InstrumentationRegistry.getInstrumentation().context
        ExponeaConfigRepository.set(applicationContext, configuration)
        val campaignIntent = createDeeplinkIntent()
        val controller = Robolectric.buildActivity(TestActivity::class.java, campaignIntent)
        controller.create()
        controller.start()
        controller.postCreate(null)
        idleThreads()
        assertFalse(Exponea.isInitialized)
        val preferences = ExponeaPreferencesImpl(applicationContext)
        val campaignRepository = CampaignRepositoryImpl(ExponeaGson.instance, preferences)
        val eventRepository = EventRepositoryImpl(applicationContext, preferences)
        idleThreads()
        val campaignEvent = campaignRepository.get()
        assertNotNull(campaignEvent)
        assertTrue(eventRepository.all().any { it.type == Constants.EventTypes.push })

        controller.resume()
        idleThreads()
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
     * Used by test testBehavior_007 (Cold start, Campaign click start, SDK init after onResume)
     */
    class TestActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_AppCompat_Light)
            Exponea.handleCampaignIntent(intent, applicationContext)
        }

        override fun onPostResume() {
            super.onPostResume()
            initExponea(context = this)
        }
    }
}
