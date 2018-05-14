package com.exponea.sdk

import android.app.Activity
import com.exponea.sdk.manager.SessionManager
import com.exponea.sdk.manager.SessionManagerImpl
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {

    private lateinit var sm: SessionManager
    private lateinit var prefs: ExponeaPreferencesImpl


    @Before
    fun init () {
        val context = RuntimeEnvironment.application.applicationContext
        prefs = ExponeaPreferencesImpl(context)
        sm = SessionManagerImpl(context, prefs)
    }

    @Test
    fun testSessionStart() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        sm.startSessionListener()
        assertEquals(-1L, prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))

        val preTime = Date().time

        // App getting focus
        controller.resume()
        assertNotEquals(-1L, prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))
        assert(Date().time >= prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))
        assert(preTime <= prefs.getLong(SessionManagerImpl.PREF_SESSION_START, -1L))

    }

}
