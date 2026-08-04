package com.exponea.sdk

import android.content.Context
import android.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.preferences.ExponeaPreferencesConstants
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaPreferencesTest {

    companion object {
        const val VAL_BOOL = "booleanValue"
        const val VAL_STRING = "stringValue"
    }

    private lateinit var prefs: ExponeaPreferences

    @Before
    fun init() {
        ExponeaPreferencesImpl.resetMigrationCache()
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
        val default = "deleted"

        prefs.setString(VAL_STRING, value)

        assertEquals(value, prefs.getString(VAL_STRING, default))
        assertEquals(true, prefs.remove(VAL_STRING))
        assertEquals(default, prefs.getString(VAL_STRING, default))
    }

    @Test
    fun defaultInstance_usesDedicatedPreferencesFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dedicatedPrefs = ExponeaPreferencesImpl(context)
        dedicatedPrefs.setString("testKey", "testValue")

        val dedicated = context.getSharedPreferences(
            ExponeaPreferencesConstants.EXPONEA_PREFERENCES_FILE,
            Context.MODE_PRIVATE
        )
        val legacy = PreferenceManager.getDefaultSharedPreferences(context)

        assertEquals("testValue", dedicated.getString("testKey", ""))
        assertFalse(legacy.contains("testKey"))
    }

    @Test
    fun namedInstance_doesNotUseDedicatedFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ExponeaPreferencesImpl(context, "EXPONEA_PUSH_TOKEN").setString("tokenKey", "v")

        val pushPrefs = context.getSharedPreferences("EXPONEA_PUSH_TOKEN", Context.MODE_PRIVATE)
        val dedicated = context.getSharedPreferences(
            ExponeaPreferencesConstants.EXPONEA_PREFERENCES_FILE,
            Context.MODE_PRIVATE
        )

        assertEquals("v", pushPrefs.getString("tokenKey", ""))
        assertFalse(dedicated.contains("tokenKey"))
    }
}
