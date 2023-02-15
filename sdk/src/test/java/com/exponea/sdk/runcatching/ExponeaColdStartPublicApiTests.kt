package com.exponea.sdk.runcatching

import com.exponea.sdk.Exponea
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.reflect.KFunction
import kotlin.test.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ExponeaColdStartPublicApiTests(
    val method: KFunction<Any>,
    val methodInvoke: () -> Any
) : ExponeaSDKTest() {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): List<Array<out Any?>> {
            return PublicApiTestCases.methods
                    .map { arrayOf(it.first, it.second) }
        }
    }

    @Before
    fun disallowSafeMode() {
        // Throws exception while invoking any public API
        Exponea.safeModeEnabled = false
    }

    @Test
    fun invokePublicMethod_withoutCachedExponeaConfiguration() {
        assertFalse(Exponea.isInitialized,
                "Ensure non-initialized SDK for method ${method.name}")
        methodInvoke()
        assertFalse(Exponea.isInitialized,
                "SDK should not be initialized without config after method ${method.name}")
    }
}
