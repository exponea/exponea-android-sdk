package com.exponea.sdk.runcatching

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RunAfterInitApiCoveredTest : ExponeaSDKTest() {

    @Test
    fun `should run block immediately when SDK is already initialized`() {
        Exponea.flushMode = FlushMode.MANUAL

        initSdk()
        assertTrue(Exponea.isInitialized)

        var called = false
        Exponea.initGate.runAfterInit { called = true }

        assertTrue(called)
        assertTrue(Exponea.initGate.afterInitCallbacks.isEmpty())
    }

    @Test
    fun `should wake deferred block after init`() {
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.isInitialized = false

        var called = false
        Exponea.initGate.runAfterInit { called = true }
        assertFalse(called)
        initSdk()
        assertTrue(called)
        assertTrue(Exponea.initGate.afterInitCallbacks.isEmpty())
    }

    @Test
    fun `should drop deferred block for stopped SDK`() {
        Exponea.isStopped = true

        var called = false
        Exponea.initGate.runAfterInit { called = true }

        assertFalse(called)
        assertEquals(0, Exponea.initGate.afterInitCallbacks.size)
    }

    @Test
    fun `should invoke deferred blocks by FIFO`() {
        Exponea.flushMode = FlushMode.MANUAL

        var brief = ""
        val expectedResult = "Hello world"
        expectedResult.forEach {
            Exponea.initGate.runAfterInit { brief += it }
        }
        initSdk()
        assertEquals(expectedResult, brief)
    }

    @Test
    fun `should invoke all deferred blocks in case of error`() {
        Exponea.flushMode = FlushMode.MANUAL

        val safeModeOrig = Exponea.safeModeEnabled
        Exponea.safeModeEnabled = true
        var runsCount = 0
        Exponea.initGate.runAfterInit { runsCount++ }
        Exponea.initGate.runAfterInit { throw RuntimeException("should be only logged") }
        Exponea.initGate.runAfterInit { runsCount++ }
        initSdk()
        Exponea.safeModeEnabled = safeModeOrig
        assertEquals(2, runsCount)
    }

    private fun initSdk() {
        Exponea.init(
            ApplicationProvider.getApplicationContext(),
            ExponeaConfiguration(integrationConfig = StreamConfig(streamId = "mock-stream-id"))
        )
    }
}
