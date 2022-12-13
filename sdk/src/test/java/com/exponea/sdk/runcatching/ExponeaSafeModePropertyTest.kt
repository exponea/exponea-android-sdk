package com.exponea.sdk.runcatching

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ExponeaSafeModePropertyTest(
    @Suppress("UNUSED_PARAMETER")
    name: String,
    val property: KProperty0<Any?>,
    val value: Any?
) : ExponeaSDKTest() {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Accessing {0} before init")
        fun data(): List<Array<out Any?>> {
            return PublicApiTestCases.properties.map { arrayOf(it.first.name, it.first, it.second) }
        }
    }

    @Before
    fun before() {
        // some tests overrides this singleton field, we need to reset it back
        Exponea.appInboxProvider = Constants.AppInbox.defaulAppInboxProvider
    }

    @Test
    fun getBeforeInit() {
        Exponea.safeModeEnabled = true
        assertFalse { Exponea.isInitialized }
        assertEquals(value, property.get())
    }

    @Test
    fun setBeforeInit() {
        Exponea.safeModeEnabled = true
        assertFalse { Exponea.isInitialized }
        if (property is KMutableProperty0<Any?>) {
            property.set(value)
        }
    }
}
