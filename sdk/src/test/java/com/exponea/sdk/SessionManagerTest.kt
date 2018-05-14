package com.exponea.sdk

import android.app.Activity
import com.exponea.sdk.manager.SessionManager
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.preferences.ExponeaPreferences
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {

    private lateinit var sm: SessionManager
    private lateinit var prefs: ExponeaPreferences


    @Before
    fun init () {
        val context = RuntimeEnvironment.application.applicationContext
        val configuration = ExponeaConfiguration()
        configuration.baseURL = "url"
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        //Set timeout a bit smaller for  testing purposes
        configuration.sessionTimeout = 2.0

        Exponea.init(context, configuration)

        sm = Exponea.component.sessionManager
        prefs = Exponea.component.preferences
        Exponea.flushMode = FlushMode.MANUAL


    }

    @Test
    fun testSessionStart() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
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

        // New sesion should be started
        newStartTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L)
        assert(previousStartTime < newStartTime)


    }

    @Test
    fun testSessionStop() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()

        //App getting focus
        controller.resume()

        val preTime = Date().time

        // App looses focus
        controller.pause()

        //Checking that stop timestamp was successfully saved
        val sessionEndTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_END, -1L)

        assertNotEquals(-1L, sessionEndTime)
        assert(preTime <= sessionEndTime)

        // User comes back and then leaves
        controller.resume()
        controller.pause()

        val newEndTime = prefs.getLong(SessionManagerImpl.PREF_SESSION_END, -1L)
        assertNotEquals(sessionEndTime, newEndTime)
        assert(sessionEndTime < newEndTime)



    }

}
