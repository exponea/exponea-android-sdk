package com.exponea.sdk.testutil

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeoutException
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
class WaitForItTest {

    @Test
    fun `should pass on main thread`() {
        waitForIt {
            it.assertTrue(true)
            it()
        }
    }

    @Test(expected = AssertionError::class)
    fun `should throw assertion error on main thread`() {
        waitForIt {
            it.assertTrue(false)
        }
    }

    @Test
    fun `should pass on other thread`() {
        waitForIt {
            thread(start=true) {
                it.assertTrue(true)
                it()
            }
        }
    }

    @Test(expected = AssertionError::class)
    fun `should throw assertion error on other thread`() {
        waitForIt {
            thread(start=true) {
                it.assertTrue(false)
            }
        }
    }

    @Test(expected = TimeoutException::class)
    fun `should timeout`() {
        waitForIt(10) {
            thread {
                Thread.sleep(1000)
                it()
            }
        }
    }

}