package com.exponea.sdk.runcatching

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.every
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
): ExponeaSDKTest() {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name="{0}")
        fun data() : List<Array<out Any?>> {
            return PublicApiTestCases.methods.map { arrayOf(it.first, it.second)}
        }
    }

    private class TestPurposeException : Exception("Exception for test purposes")

    @Before
    fun before() {
        mockkConstructor(ExponeaComponent::class)
    }

    fun makeExponeaThrow() {
        // let's mock most of ExponeaComponent to make sure any public method throws after init
        every {anyConstructed<ExponeaComponent>().eventRepository } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().pushManager } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().personalizationManager } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().customerIdsRepository } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().flushManager } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().fetchManager } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().connectionManager } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().fcmManager } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().sessionManager } throws TestPurposeException()
        every {anyConstructed<ExponeaComponent>().campaignRepository } throws TestPurposeException()
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
        Exponea.init(ApplicationProvider.getApplicationContext())
        Exponea.safeModeEnabled = true
        makeExponeaThrow()
        lambda()
    }

    @Test(expected = TestPurposeException::class)
    fun callAfterInitWithSafeModeDisabled() {
        Exponea.init(ApplicationProvider.getApplicationContext())
        Exponea.safeModeEnabled = false
        makeExponeaThrow()
        lambda()
    }
}
