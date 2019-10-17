package com.exponea.sdk.runcatching

import com.exponea.sdk.Exponea
import com.exponea.sdk.testutil.ExponeaSDKTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0
import kotlin.test.assertEquals
import kotlin.test.assertFalse


@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ExponeaSafeModePropertyTest(
    name: String,
    val property: KProperty0<Any?>,
    val value: Any?
): ExponeaSDKTest() {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name="Accessing {0} before init")
        fun data() : List<Array<out Any?>> {
            return PublicApiTestCases.properties.map { arrayOf(it.first.name, it.first, it.second)}
        }
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