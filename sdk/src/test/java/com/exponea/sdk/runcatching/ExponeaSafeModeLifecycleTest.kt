package com.exponea.sdk.runcatching

import android.app.Activity
import android.os.Bundle
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.testutil.ExponeaSDKTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
internal class ExponeaSafeModeLifecycleTest : ExponeaSDKTest() {
    private lateinit var controller: ActivityController<TestActivity>

    @Before
    fun before() {
        ExponeaExceptionThrowing.prepareExponeaToThrow()
        controller = Robolectric.buildActivity(TestActivity::class.java)
        controller.create()
    }

    @Test
    fun `should not throw internal error on onResume when in safe mode`() {
        Exponea.safeModeEnabled = true
        ExponeaExceptionThrowing.makeExponeaThrow()
        controller.resume()
    }

    @Test(expected = ExponeaExceptionThrowing.TestPurposeException::class)
    fun `should throw internal error on onResume when not in safe mode`() {
        Exponea.safeModeEnabled = false
        ExponeaExceptionThrowing.makeExponeaThrow()
        try {
            controller.resume()
        } catch (e: RuntimeException) { // robolectric wraps exception into runtime exception
            if (e.cause != null) throw e.cause!!
        }
    }

    @Test
    fun `should not throw internal error on onPause when in safe mode`() {
        Exponea.safeModeEnabled = true
        controller.resume()
        ExponeaExceptionThrowing.makeExponeaThrow()
        controller.pause()
    }

    @Test(expected = ExponeaExceptionThrowing.TestPurposeException::class)
    fun `should throw internal error on onPause when not in safe mode`() {
        Exponea.safeModeEnabled = false
        controller.resume()
        ExponeaExceptionThrowing.makeExponeaThrow()
        controller.pause()
    }

    class TestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            skipInstallEvent()
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.init(applicationContext, ExponeaConfiguration())
        }
    }
}
