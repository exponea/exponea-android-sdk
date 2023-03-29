package com.exponea.sdk.runcatching

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import java.util.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WaitForInitApiCoveredTest : ExponeaSDKTest() {

    @Before
    fun overrideThreadBehaviour() {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Main)
    }

    @After
    fun restoreThreadBehaviour() {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Default)
    }

    @Test
    fun `all Init awaiting methods should be testable`() {
        val publicMethods = PublicApiTestCases.methods.map { it.first }
        assertTrue {
            PublicApiTestCases.awaitInitMethods.all {
                publicMethods.contains(it)
            }
        }
    }

    @Test
    fun `all Init awaiting methods cannot be SDKless`() {
        assertTrue {
            PublicApiTestCases.awaitInitMethods.all {
                !PublicApiTestCases.sdkLessMethods.contains(it)
            }
        }
    }

    @Test
    fun `should wake pending callbacks after init`() {
        PublicApiTestCases.initMethods.forEach {
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.isInitialized = false
            skipInstallEvent()
            var called = false
            var initRuns = false
            Exponea.initGate.waitForInitialize {
                called = true
                assertTrue(initRuns)
            }
            // run init
            initRuns = true
            it.second.invoke()
            // check if trigger has been called
            assertTrue(called)
            assertTrue(Exponea.initGate.afterInitCallbacks.isEmpty())
        }
    }

    @Test
    fun `listed API methods should wait for SDK init`() {
        PublicApiTestCases.methods
            .filter { PublicApiTestCases.awaitInitMethods.contains(it.first) }
            .forEach {
                Exponea.flushMode = FlushMode.MANUAL
                Exponea.isInitialized = false
                skipInstallEvent()
                // invoke method
                it.second.invoke()
                assertEquals(1, Exponea.initGate.afterInitCallbacks.size,
                    "Method 'Exponea.${it.first.name}' didn't wait for full init"
                )
                // !!! do not run init, it'll start method in real and we have no power to detect when it is finished.
                // We are fine, that method has been added to pending callbacks
                Exponea.initGate.clear()
            }
    }

    @Test
    fun `should drop pending callbacks after anonymize`() {
        PublicApiTestCases.initMethods.forEach {
            Exponea.flushMode = FlushMode.MANUAL
            Exponea.isInitialized = false
            skipInstallEvent()
            // run init
            it.second.invoke()
            // Simulate non-init, callback will be kept
            Exponea.isInitialized = false
            var called = false
            Exponea.initGate.waitForInitialize {
                called = true
            }
            // Return init state for anonymize method; plus callbacks are not triggered this way
            Exponea.isInitialized = true
            assertEquals(1, Exponea.initGate.afterInitCallbacks.size,
                "ExponeaInitManager didn't wait for trigger"
            )
            Exponea.anonymize()
            // check if trigger has not been called AND callbacks are dropped
            assertFalse(called)
            assertTrue(Exponea.initGate.afterInitCallbacks.isEmpty())
        }
    }

    @Test
    fun `should invoke pending callbacks by FIFO`() {
        Exponea.flushMode = FlushMode.MANUAL
        skipInstallEvent()
        val randomized = Random()
        var brief = ""
        val expectedResult = "Hello world"
        expectedResult.forEach {
            Exponea.initGate.waitForInitialize {
                Thread.sleep(500 + (if (randomized.nextBoolean()) 500L else 0))
                brief += it
            }
        }
        // run init
        PublicApiTestCases.initMethods.get(1).second.invoke()
        // check if brief has been written as expected
        assertEquals(expectedResult, brief)
    }

    @Test
    fun `should invoke all pending callbacks in case or error`() {
        Exponea.flushMode = FlushMode.MANUAL
        skipInstallEvent()
        val safeModeOrig = Exponea.safeModeEnabled
        Exponea.safeModeEnabled = true
        var runsCount = 0
        Exponea.initGate.waitForInitialize {
            runsCount++
        }
        Exponea.initGate.waitForInitialize {
            throw java.lang.RuntimeException("should be only logged")
        }
        Exponea.initGate.waitForInitialize {
            runsCount++
        }
        // run init
        PublicApiTestCases.initMethods.get(1).second.invoke()
        Exponea.safeModeEnabled = safeModeOrig
        assertEquals(2, runsCount)
    }
}
