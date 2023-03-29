package com.exponea.sdk.runcatching

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.slot
import kotlin.reflect.KFunction
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
                .filter { PublicApiTestCases.sdkLessMethods.contains(it.first) }
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
        mockkConstructor(EventManagerImpl::class)
        val processTrackSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>()
                .processTrack(any(), any(), any(), capture(processTrackSlot), any())
        } just Runs
        methodInvoke()
        assertFalse(processTrackSlot.isCaptured,
            "Tracking should not be invoked without config for method ${method.name}")
    }

    @Test
    fun invokePublicMethod_withCachedExponeaConfiguration() {
        assertFalse(Exponea.isInitialized,
            "Ensure non-initialized SDK for method ${method.name}")
        ExponeaConfigRepository.set(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(
                projectToken = "project-token",
                authorization = "Token mock-auth",
                baseURL = "https://api.exponea.com"
            )
        )
        mockkConstructor(EventManagerImpl::class)
        val processTrackSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>()
                .processTrack(any(), any(), any(), capture(processTrackSlot), any())
        } just Runs
        methodInvoke()
        assertTrue(processTrackSlot.isCaptured,
            "Tracking should be invoked with config for method ${method.name}")
    }
}
