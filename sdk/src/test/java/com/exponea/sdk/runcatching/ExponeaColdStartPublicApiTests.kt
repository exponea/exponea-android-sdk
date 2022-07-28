package com.exponea.sdk.runcatching

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.runcatching.PublicApiTestCases.autoInitializingMethods
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.Before
import kotlin.reflect.KFunction
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import kotlin.test.fail

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

    @Test
    fun invokePublicMethod_withCachedExponeaConfiguration() {
        assertFalse(Exponea.isInitialized,
                "Ensure non-initialized SDK for method ${method.name}")
        Exponea.flushMode = FlushMode.MANUAL
        ExponeaConfigRepository.set(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        methodInvoke()
        if (autoInitializingMethods.contains(method)) {
            assertTrue(Exponea.isInitialized, "SDK has to be initialized after method ${method.name}")
        } else {
            assertFalse(Exponea.isInitialized,
                    "SDK should not be initialized without config after method ${method.name}")
        }
    }

}
