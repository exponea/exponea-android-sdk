package com.exponea.sdk

import android.app.Activity
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.manager.SessionManager
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.preferences.ExponeaPreferences
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {

    companion object {
        val configuration = ExponeaConfiguration()

        @BeforeClass @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestBasicAuthentication"
            configuration.sessionTimeout = 2.0
        }
    }

    private lateinit var sm: SessionManager
    private lateinit var prefs: ExponeaPreferences

    @Before
    fun prepareForTest() {

        val context = RuntimeEnvironment.application

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.isAutomaticSessionTracking = false

        sm = Exponea.component.sessionManager
        prefs = Exponea.component.preferences
    }

    @Test
    fun testSessionStart() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()

        // Since we have disabled automatic tracking, we have to set listeners manually
        sm.startSessionListener()
        assertEquals(-1L, prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))

        val preTime = Date().time

        // App getting focus
        controller.resume()

        // Checking that start timestamp was successfully saved
        assertNotEquals(-1L, prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))
        assert(Date().time >= prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))
        assert(preTime <= prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))

        var previousStartTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L)

        controller.pause()
        controller.resume()
        var newStartTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L)

        // App regained focus too quickly, old session should be resumed
        assertEquals(previousStartTime, newStartTime)

        // Sleep to wait for the timeout to end
        previousStartTime = newStartTime
        controller.pause()
        Thread.sleep(2003)
        controller.resume()

        // New session should be started
        newStartTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L)
        assert(previousStartTime < newStartTime)
    }

    @Test
    fun testSessionStop() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()

        // App getting focus
        controller.resume()

        val preTime = Date().time

        // App looses focus
        controller.pause()

        // Checking that stop timestamp was successfully saved
        val sessionEndTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_END, -1L)

        assertNotEquals(-1L, sessionEndTime)
        assert(preTime <= sessionEndTime)

        // User comes back and then leaves
        controller.resume()
        controller.pause()

        // New session end time
        val newEndTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_END, -1L)
        assertNotEquals(sessionEndTime, newEndTime)
        assert(sessionEndTime < newEndTime)
    }

    @Test
    fun testStopTracking() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        controller.resume()

        // Session wont be recorded until startSessionListenerCalled()
        var sessionStartTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L)
        assertEquals(-1L, sessionStartTime)

        // Starting our listener
        sm.startSessionListener()
        controller.resume()
        sessionStartTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L)
        assertNotEquals(-1L, sessionStartTime)
        // Listener's state saved in SP
        assertTrue { prefs.getBoolean(SessionManagerImpl.PREF_SESSION_AUTO_TRACK, false) }

        // Stopping session listener
        sm.stopSessionListener()
        assertTrue { !prefs.getBoolean(SessionManagerImpl.PREF_SESSION_AUTO_TRACK, true) }

        // App looses focus, but session's end won't be recorded
        controller.pause()
        assertEquals(-1L, prefs.getLong(SessionManagerImpl.PREF_SESSION_END, -1L))

        // As well as the session start, until we start listeners again
        controller.resume()
        assertEquals(sessionStartTime, prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))
    }
}
