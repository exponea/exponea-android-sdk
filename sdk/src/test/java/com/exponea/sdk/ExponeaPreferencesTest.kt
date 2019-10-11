package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.testutil.ExponeaSDKTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
internal class ExponeaPreferencesTest: ExponeaSDKTest() {

    companion object {
        const val VAL_BOOL = "booleanValue"
        const val VAL_STRING = "stringValue"
    }

    private lateinit var prefs: ExponeaPreferences

    @Before
    fun init() {
        prefs = ExponeaPreferencesImpl(ApplicationProvider.getApplicationContext())
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