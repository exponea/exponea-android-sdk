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
import com.exponea.sdk.util.currentTimeSeconds
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class SessionManagerTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestTokenAuthentication"
            configuration.sessionTimeout = 2.0
        }
    }

    private lateinit var sm: SessionManager
    private lateinit var prefs: ExponeaPreferences

    @Before
    fun prepareForTest() {

        val context = ApplicationProvider.getApplicationContext<Context>()
        skipInstallEvent()
        Exponea.init(context, configuration)
        waitUntilFlushed()
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
        assertEquals(-1.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))

        val preTime = currentTimeSeconds()

        // App getting focus
        controller.resume()
        Thread.sleep(100)

        // Checking that start timestamp was successfully saved
        assertNotEquals(-1.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))
        assert(currentTimeSeconds() >= prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))
        assert(preTime <= prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))

        var previousStartTime = prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0)

        controller.pause()
        controller.resume()
        var newStartTime = prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0)

        // App regained focus too quickly, old session should be resumed
        assertEquals(previousStartTime, newStartTime)

        // Sleep to wait for the timeout to end
        previousStartTime = newStartTime
        controller.pause()
        Thread.sleep(2003)
        controller.resume()

        // New session should be started
        newStartTime = prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0)
        assert(previousStartTime < newStartTime)
    }

    @Test
    fun testSessionStop() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()

        // App getting focus
        controller.resume()

        val preTime = currentTimeSeconds()

        // App looses focus
        controller.pause()

        // Checking that stop timestamp was successfully saved
        val sessionEndTime = prefs.getDouble(SessionManagerImpl.PREF_SESSION_END, -1.0)

        assertNotEquals(-1.0, sessionEndTime)
        assert(preTime <= sessionEndTime)

        // User comes back and then leaves
        controller.resume()
        Thread.sleep(1000)
        controller.pause()

        // New session end time
        val newEndTime = prefs.getDouble(SessionManagerImpl.PREF_SESSION_END, -1.0)
        assertNotEquals(sessionEndTime, newEndTime)
        assert(sessionEndTime < newEndTime)
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
        assertTrue { prefs.getBoolean(SessionManagerImpl.PREF_SESSION_AUTO_TRACK, false) }

        // Stopping session listener
        sm.stopSessionListener()
        assertTrue { !prefs.getBoolean(SessionManagerImpl.PREF_SESSION_AUTO_TRACK, true) }

        // App looses focus, but session's end won't be recorded
        controller.pause()
        assertEquals(-1.0, prefs.getDouble(SessionManagerImpl.PREF_SESSION_END, -1.0))

        // As well as the session start, until we start listeners again
        controller.resume()
        assertEquals(sessionStartTime, prefs.getDouble(SessionManagerImpl.PREF_SESSION_START, -1.0))
    }
}
