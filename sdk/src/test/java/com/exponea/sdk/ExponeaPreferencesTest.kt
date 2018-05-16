package com.exponea.sdk

import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ExponeaPreferencesTest {

    companion object {
        const val VAL_BOOL = "booleanValue"
        const val VAL_STRING = "stringValue"
    }

    lateinit var prefs: ExponeaPreferences

    @Before
    fun init() {
        prefs = ExponeaPreferencesImpl(RuntimeEnvironment.application.applicationContext)
    }

    @Test
    fun setBoolean_ShouldPass() {
        val toSet = false
        prefs.setBoolean(VAL_BOOL, toSet)
        assertEquals(toSet, prefs.getBoolean(VAL_BOOL, true))
    }

    @Test
    fun setString_ShouldPass() {
        val toSet = "sampleString"
        prefs.setString(VAL_STRING, toSet)
        assertEquals(toSet, prefs.getString(VAL_STRING, "wrong one"))
    }

    @Test
    fun remove_ShouldPass() {
        val value = "someOtherString"
        prefs.setString(VAL_STRING, value)
        assertEquals(value, prefs.getString(VAL_STRING, "deleted"))
        assertEquals(true, prefs.remove(VAL_STRING))
        assertEquals("deleted", prefs.getString(VAL_STRING, "deleted"))
    }

}