package com.exponea.sdk.tracking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.models.Constants
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.runInSingleThread
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
internal class CampaignSessionTests_006 : CampaignSessionTests_Base() {

    /**
     * Hot Start with Resumed Session, Campaign click start, SDK init in onResume
     */
    @Test
    fun testBehavior_006() = runInSingleThread { idleThreads ->
        // first run will initialize SDK
        val firstRun = Robolectric.buildActivity(TestActivity::class.java)
        firstRun.create(Bundle.EMPTY)
        firstRun.start()
        firstRun.postCreate(null)
        firstRun.resume()
        firstRun.pause()
        firstRun.stop()
        firstRun.destroy()

        // second run will handle Campaign Intent, but session will be resumed
        val campaignIntent = createDeeplinkIntent()
        val secondRun = Robolectric.buildActivity(TestActivity::class.java, campaignIntent)
        secondRun.create()
        idleThreads()
        assertTrue(Exponea.isInitialized)
        val campaignEvent = Exponea.componentForTesting.campaignRepository.get()
        assertNotNull(campaignEvent)
        assertTrue(Exponea.componentForTesting.eventRepository.all().any { it.type == Constants.EventTypes.push })

        secondRun.start()
        secondRun.postCreate(null)
        secondRun.resume()

        assertNull(Exponea.componentForTesting.campaignRepository.get())
        assertEquals(1, Exponea.componentForTesting.eventRepository.all().count {
            it.type == Constants.EventTypes.sessionStart
        }, "Only single session_start has to exists")
        val sessionEvent = Exponea.componentForTesting.eventRepository.all().find {
            it.type == Constants.EventTypes.sessionStart
        }
        assertNotNull(sessionEvent)
        assertNotNull(sessionEvent.properties)
        assertNull(sessionEvent.properties!!["location"])
        assertNull(sessionEvent.properties!!["utm_source"])
        assertNull(sessionEvent.properties!!["utm_campaign"])
        assertNull(sessionEvent.properties!!["utm_content"])
        assertNull(sessionEvent.properties!!["utm_term"])

        val hasAnySessionEnd = Exponea.componentForTesting.eventRepository.all().any {
            it.type == Constants.EventTypes.sessionEnd
        }
        assertFalse(hasAnySessionEnd)
        val sessionStartEvents = Exponea.componentForTesting.eventRepository.all().filter {
            it.type == Constants.EventTypes.sessionStart
        }
        assertEquals(1, sessionStartEvents.size)
    }

    /**
     * Used by test testBehavior_006 (Hot Start with Resumed Session, Campaign click start, SDK init in onResume)
     */
    class TestActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_AppCompat)
            Exponea.handleCampaignIntent(intent, applicationContext)
        }

        override fun onResume() {
            super.onResume()
            initExponea(applicationContext)
        }
    }
}
