package com.exponea.sdk.runcatching

import com.exponea.sdk.Exponea
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.every
import kotlin.reflect.KFunction
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ExponeaSafeModeInitTest(
    @Suppress("UNUSED_PARAMETER")
    method: KFunction<Any>,
    val lambda: () -> Any
) : ExponeaSDKTest() {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): List<Array<out Any?>> {
            return PublicApiTestCases.initMethods.map { arrayOf(it.first, it.second) }
        }
    }

    @Test
    fun callInitWithoutExceptionWithSafeModeEnabled() {
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.safeModeEnabled = true
        lambda()
        assertTrue(Exponea.isInitialized)
    }

    @Test
    fun callInitWithExceptionWithSafeModeEnabled() {
        mockkConstructorFix(TelemetryManager::class)
        every {
            anyConstructed<TelemetryManager>().start()
        } throws ExponeaExceptionThrowing.TestPurposeException()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.safeModeEnabled = true
        lambda()
        assertFalse(Exponea.isInitialized)
    }

    @Test(expected = ExponeaExceptionThrowing.TestPurposeException::class)
    fun callInitWithExceptionWithSafeModeDisabled() {
        mockkConstructorFix(TelemetryManager::class)
        every {
            anyConstructed<TelemetryManager>().start()
        } throws ExponeaExceptionThrowing.TestPurposeException()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.safeModeEnabled = false
        assertFalse(Exponea.isInitialized)
        lambda()
        assertFalse(Exponea.isInitialized)
    }
}
