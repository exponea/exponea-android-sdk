package com.exponea.sdk.tracking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.repository.ExponeaConfigRepository
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
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CampaignSessionTests_009 : CampaignSessionTests_Base() {

    /**
     * Hot start with resume session, campaign click start, SDK init after onResume
     */
    @Test
    fun testBehavior_009() = runInSingleThread { idleThreads ->
        ExponeaConfigRepository.set(InstrumentationRegistry.getInstrumentation().context, configuration)
        // first run will initialize SDK
        val firstRun = buildActivity(TestActivity::class.java)
        firstRun.create()
        firstRun.start()
        firstRun.postCreate(null)
        firstRun.resume()
        firstRun.postResume()
        firstRun.visible()
        firstRun.topActivityResumed(true)
        idleThreads()
        val sessionStartRecord = Exponea.componentForTesting.eventRepository.all().last {
            it.type == Constants.EventTypes.sessionStart
        }
        firstRun.pause()
        firstRun.stop()
        firstRun.destroy()
        idleThreads()

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
        secondRun.postResume()
        secondRun.visible()
        secondRun.topActivityResumed(true)
        idleThreads()

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
        assertEquals(sessionStartRecord.id, sessionStartEvents.first().id)
    }

    /**
     * Used by test testBehavior_009 (Hot start with resume session, campaign click start, SDK init after onResume)
     */
    class TestActivity : AppCompatActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(androidx.appcompat.R.style.Theme_AppCompat)
            Exponea.handleCampaignIntent(intent, applicationContext)
        }

        override fun onPostResume() {
            super.onPostResume()
            initExponea(context = this)
        }
    }
}
