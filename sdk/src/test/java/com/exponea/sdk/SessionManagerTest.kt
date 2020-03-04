package com.exponea.sdk

import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.BackgroundTimerManager
import com.exponea.sdk.manager.EventManager
import com.exponea.sdk.manager.SessionManager
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.models.EventType
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.CampaignRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import java.util.Date
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SessionManagerTest : ExponeaSDKTest() {
    private lateinit var sm: SessionManager
    private lateinit var eventManager: EventManager

    @Before
    fun prepareForTest() {
        mockkObject(Exponea)
        every { Exponea.sessionTimeout } returns 20.0

        mockkConstructor(Date::class)
        every { anyConstructed<Date>().time } returns 10 * 1000 // mock current time

        val context = ApplicationProvider.getApplicationContext<Context>()
        eventManager = mockk()
        every { eventManager.track(any(), any(), any(), any()) } just Runs

        val backgroundTimer = mockk<BackgroundTimerManager>()
        every { backgroundTimer.startTimer() } just Runs
        every { backgroundTimer.stopTimer() } just Runs

        val campaignRepository = mockk<CampaignRepository>()
        every { campaignRepository.get() } returns null
        every { campaignRepository.clear() } returns true

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        val prefs = ExponeaPreferencesImpl(context)

        sm = SessionManagerImpl(context, prefs, campaignRepository, eventManager, backgroundTimer)
    }

    @After
    fun unmock() {
        // mockk has a problem when it sometimes throws an exception, in that case just try again
        try {
            unmockkAll()
        } catch (error: ConcurrentModificationException) {
            unmock()
        }
    }

    @Test
    fun `should track session start if initialized before onResume`() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()
        controller.resume()
        controller.pause()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        confirmVerified(eventManager)
    }

    @Test
    fun `should track session start if initialized after onResume`() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.resume()
        sm.startSessionListener()
        controller.pause()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        confirmVerified(eventManager)
    }

    @Test
    fun `should resume running session if withing session timeout`() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()
        controller.resume()
        every { anyConstructed<Date>().time } returns 100 * 1000 // app paused after 90 sec
        controller.pause()
        every { anyConstructed<Date>().time } returns 110 * 1000 // app resumed after being backgrounded for 10 sec
        controller.resume()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        confirmVerified(eventManager)
    }

    @Test
    fun `should start new session if outside of session timeout`() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()
        controller.resume()
        every { anyConstructed<Date>().time } returns 100 * 1000 // app paused after 90 sec
        controller.pause()
        every { anyConstructed<Date>().time } returns 120 * 1000 // app resumed after being backgrounded for 20 sec
        controller.resume()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
            eventManager.track("session_end", 100.0, any(), EventType.SESSION_END)
            eventManager.track("session_start", 120.0, any(), EventType.SESSION_START)
        }
        confirmVerified(eventManager)
    }

    @Test
    fun `should stop tracking`() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()
        controller.resume()
        every { anyConstructed<Date>().time } returns 100 * 1000 // app paused after 90 sec
        sm.stopSessionListener()
        controller.pause()
        every { anyConstructed<Date>().time } returns 120 * 1000 // app resumed after being backgrounded for 20 sec
        controller.resume()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        confirmVerified(eventManager)
    }
}
