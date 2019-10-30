package com.exponea.sdk.runcatching

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import kotlin.reflect.KFunction
import kotlin.test.assertFalse

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ExponeaSafeModeMethodTest(
    method: KFunction<Any>,
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
        mockkConstructor(ExponeaComponent::class)
    }

    @After
    fun after() {
        unmockkConstructor(ExponeaComponent::class)
        waitUntilFlushed()
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
        Exponea.init(ApplicationProvider.getApplicationContext())
        Exponea.safeModeEnabled = true
        ExponeaExceptionThrowing.makeExponeaThrow()
        lambda()
    }

    @Test(expected = ExponeaExceptionThrowing.TestPurposeException::class)
    fun callAfterInitWithSafeModeDisabled() {
        skipInstallEvent()
        Exponea.init(ApplicationProvider.getApplicationContext())
        Exponea.safeModeEnabled = false
        ExponeaExceptionThrowing.makeExponeaThrow()
        lambda()
    }
}
