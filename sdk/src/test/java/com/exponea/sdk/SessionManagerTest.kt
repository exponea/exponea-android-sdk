package com.exponea.sdk

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.SessionManager
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SessionManagerTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestTokenAuthentication"
            configuration.sessionTimeout = 5.0
        }
    }

    private lateinit var sm: SessionManager
    private lateinit var prefs: ExponeaPreferences

    @Before
    fun prepareForTest() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
        Exponea.isAutomaticSessionTracking = false

        sm = Exponea.component.sessionManager
        prefs = Exponea.component.preferences
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
    fun testSessionStart() {
        mockkConstructor(Date::class)
        every { anyConstructed<Date>().time } returns 10 * 1000 // mock current time
        val controller = Robolectric.buildActivity(Activity::class.java).create()

        // Since we have disabled automatic tracking, we have to set listeners manually
        sm.startSessionListener()
        assertEquals(-1.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))

        // App getting focus
        controller.resume()
        // Checking that start timestamp was successfully saved
        assertEquals(10.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))

        every { anyConstructed<Date>().time } returns 60 * 1000 // advance in time
        controller.pause()
        controller.resume()
        // Session start did not change, we in session timeout window
        assertEquals(10.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))

        controller.pause()
        every { anyConstructed<Date>().time } returns 110 * 1000 // advance in time past session timeout
        controller.resume()
        assertEquals(110.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))
    }

    @Test
    fun testSessionStop() {
        mockkConstructor(Date::class)
        every { anyConstructed<Date>().time } returns 10 * 1000
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()

        // App getting focus
        controller.resume()

        // App looses focus
        controller.pause()

        assertEquals(10.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_END, -1.0))

        // User comes back and then leaves
        controller.resume()
        every { anyConstructed<Date>().time } returns 20 * 1000
        controller.pause()
        assertEquals(20.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_END, -1.0))
    }

    @Test
    fun testStopTracking() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.resume()

        // Session wont be recorded until startSessionListenerCalled()
        var sessionStartTime = prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0)
        assertEquals(-1.0, sessionStartTime)

        // Starting our listener
        sm.startSessionListener()
        controller.resume()
        sessionStartTime = prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0)
        assertNotEquals(-1.0, sessionStartTime)
        // Listener's state saved in SP

        // Stopping session listener
        sm.stopSessionListener()

        // App looses focus, but session's end won't be recorded
        controller.pause()
        assertEquals(-1.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_END, -1.0))

        // As well as the session start, until we start listeners again
        controller.resume()
        assertEquals(sessionStartTime, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))

        controller.pause()
        sm.startSessionListener()
        controller.resume()
        assertNotEquals(sessionStartTime, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))
    }
}
