package com.exponea.sdk.manager

import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.EventType
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.CampaignRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.mocks.MockApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import java.util.Date
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MockApplication::class)
internal class SessionManagerTest : ExponeaSDKTest() {
    private lateinit var eventManager: EventManager
    private lateinit var backgroundTimer: BackgroundTimerManager
    private lateinit var campaignRepository: CampaignRepository
    private lateinit var prefs: ExponeaPreferencesImpl

    @Before
    fun prepareForTest() {
        mockkObject(Exponea)
        every { Exponea.sessionTimeout } returns 20.0

        mockkConstructorFix(Date::class)
        every { anyConstructed<Date>().time } returns 10 * 1000 // mock current time

        val context = ApplicationProvider.getApplicationContext<Context>()
        eventManager = mockk()
        every { eventManager.track(any(), any(), any(), any()) } just Runs

        backgroundTimer = mockk<BackgroundTimerManager>()
        every { backgroundTimer.startTimer() } just Runs
        every { backgroundTimer.stopTimer() } just Runs

        campaignRepository = mockk<CampaignRepository>()
        every { campaignRepository.get() } returns null
        every { campaignRepository.clear() } returns true

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        prefs = ExponeaPreferencesImpl(context)
    }

    @Test
    fun `should track session start if initialized before onResume`() {
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext())
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.start()
        controller.postCreate(null)
        sm.startSessionListener()
        controller.resume()
        controller.pause()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        confirmVerified(eventManager)
    }

    @Test
    fun `should track session start if initialized after onResume with Activity`() {
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext())
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.start()
        controller.postCreate(null)
        controller.resume()
        sm.startSessionListener()
        verify(exactly = 1) {
            eventManager.track("session_start", any(), any(), EventType.SESSION_START)
        }
        confirmVerified(eventManager)
    }

    @Test
    fun `should track session start immediately if initialized after onResume with AppCompatActivity`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java).create()
        controller.start()
        controller.postCreate(null)
        controller.resume()
        // if we intitialize session manager with resumed activity, it will track session start right away
        val sm = buildSessionManager(controller.get())
        sm.startSessionListener()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        confirmVerified(eventManager)
    }

    private fun buildSessionManager(
        context: Context,
        manualSessionAutoClose: Boolean = false
    ) = SessionManagerImpl(
        context,
        prefs,
        campaignRepository,
        eventManager,
        backgroundTimer,
        manualSessionAutoClose
    )

    @Test
    fun `should resume running session if withing session timeout`() {
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext())
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.start()
        controller.postCreate(null)
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
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext())
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.start()
        controller.postCreate(null)
        sm.startSessionListener()
        controller.resume()
        every { anyConstructed<Date>().time } returns 100 * 1000 // app paused after 90 sec
        controller.pause()
        controller.stop()
        every { anyConstructed<Date>().time } returns 120 * 1000 // app resumed after being backgrounded for 20 sec
        controller.start()
        controller.postCreate(null)
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
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext())
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.start()
        controller.postCreate(null)
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

    @Test
    fun `should close active session when multiple manual session started`() {
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext(), true)
        sm.trackSessionStart(10.0)
        sm.trackSessionStart(20.0)
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        verify(exactly = 1) {
            eventManager.track("session_end", 20.0, any(), EventType.SESSION_END)
        }
        verify(exactly = 1) {
            eventManager.track("session_start", 20.0, any(), EventType.SESSION_START)
        }
    }

    @Test
    fun `should not close active session when multiple manual session started and autoClose is off`() {
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext(), false)
        sm.trackSessionStart(10.0)
        sm.trackSessionStart(20.0)
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        verify(exactly = 0) {
            eventManager.track("session_end", any(), any(), EventType.SESSION_END)
        }
        verify(exactly = 1) {
            eventManager.track("session_start", 20.0, any(), EventType.SESSION_START)
        }
    }

    @Test
    fun `should not close active automatic session when another manual session start is tracked - autoClose off`() {
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext(), false)
        every { anyConstructed<Date>().time } returns 10 * 1000
        sm.startSessionListener()
        sm.onSessionStart()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        sm.trackSessionStart(20.0)
        verify(exactly = 1) {
            eventManager.track("session_start", 20.0, any(), EventType.SESSION_START)
        }
        verify(exactly = 0) {
            eventManager.track("session_end", any(), any(), EventType.SESSION_END)
        }
    }

    @Test
    fun `should not close active automatic session when another manual session start is tracked - autoClose on`() {
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext(), true)
        every { anyConstructed<Date>().time } returns 10 * 1000
        sm.startSessionListener()
        sm.onSessionStart()
        verify(exactly = 1) {
            eventManager.track("session_start", 10.0, any(), EventType.SESSION_START)
        }
        sm.trackSessionStart(20.0)
        verify(exactly = 1) {
            eventManager.track("session_start", 20.0, any(), EventType.SESSION_START)
        }
        verify(exactly = 0) {
            eventManager.track("session_end", any(), any(), EventType.SESSION_END)
        }
    }

    @Test
    fun `should not track session start if SDK is stopped`() {
        every { Exponea.isStopped } returns true
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext())
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.start()
        controller.postCreate(null)
        sm.startSessionListener()
        controller.resume()
        controller.pause()
        verify(exactly = 0) {
            eventManager.track("session_start", any(), any(), any())
        }
        confirmVerified(eventManager)
    }

    @Test
    fun `should not track session start nor session end if SDK is stopped`() {
        every { Exponea.isStopped } returns true
        val sm = buildSessionManager(ApplicationProvider.getApplicationContext())
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.start()
        controller.postCreate(null)
        sm.startSessionListener()
        controller.resume()
        every { anyConstructed<Date>().time } returns 100 * 1000 // app paused after 90 sec
        controller.pause()
        controller.stop()
        every { anyConstructed<Date>().time } returns 120 * 1000 // app resumed after being backgrounded for 20 sec
        controller.start()
        controller.postCreate(null)
        controller.resume()
        verify(exactly = 0) {
            eventManager.track("session_start", any(), any(), any())
            eventManager.track("session_end", any(), any(), any())
        }
        confirmVerified(eventManager)
    }
}
