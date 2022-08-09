package com.exponea.sdk.runcatching

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.runcatching.ExponeaExceptionThrowing.TestPurposeException
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.reflect.KFunction
import kotlin.test.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ExponeaSafeModeMethodTest(
    val method: KFunction<Any>,
    val lambda: () -> Any
) : ExponeaSDKTest() {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): List<Array<out Any?>> {
            return PublicApiTestCases.methods.map { arrayOf(it.first, it.second) }
        }
    }

    @Before
    fun before() {
        ExponeaExceptionThrowing.prepareExponeaToThrow()
    }

    @Test
    fun callBeforeInit() {
        Exponea.safeModeEnabled = true
        assertFalse { Exponea.isInitialized }
        lambda()
    }

    @Test
    fun callAfterInitWithSafeModeEnabled() {
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        Exponea.safeModeEnabled = true
        ExponeaExceptionThrowing.makeExponeaThrow()
        lambda()
    }

    @Test(expected = ExponeaExceptionThrowing.TestPurposeException::class)
    fun callAfterInitWithSafeModeDisabled() {
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration())
        Exponea.safeModeEnabled = false
        ExponeaExceptionThrowing.makeExponeaThrow()
        lambda()
        if (method == Exponea::isExponeaPushNotification) {
            // Note: cannot throw TestPurposeException because it is not accessing SDK in any way
            // we kept invocation of method in this test for check of any other exception/error possibility
            // but TestPurposeException has to be simulated for test pass
            throw TestPurposeException()
        }
    }
}
