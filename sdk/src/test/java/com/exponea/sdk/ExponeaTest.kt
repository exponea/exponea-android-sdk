package com.exponea.sdk

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.telemetry.TelemetryUtility
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.mocks.DebugMockApplication
import com.exponea.sdk.testutil.mocks.ReleaseMockApplication
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class ExponeaTest : ExponeaSDKTest() {
    @Test
    fun `should get null as customer cookie before initialized`() {
        assertNull(Exponea.customerCookie)
    }

    @Test
    fun `should get customer cookie after initialized`() {
        initSdk()
        assertNotNull(Exponea.customerCookie)
    }

    @Test
    fun `should track telemetry for init`() {
        mockkConstructorFix(TelemetryManager::class)
        val telemetryTelemetryEventSlot = slot<com.exponea.sdk.telemetry.model.TelemetryEvent>()
        val telemetryPropertiesSlot = slot<MutableMap<String, String>>()
        every {
            anyConstructed<TelemetryManager>().reportEvent(
                capture(telemetryTelemetryEventSlot),
                capture(telemetryPropertiesSlot)
            )
        } just Runs
        Exponea.telemetry = TelemetryManager(ApplicationProvider.getApplicationContext())
        initSdk()
        assertTrue(telemetryTelemetryEventSlot.isCaptured)
        val capturedEventType = telemetryTelemetryEventSlot.captured
        assertNotNull(capturedEventType)
        assertEquals(com.exponea.sdk.telemetry.model.TelemetryEvent.SDK_CONFIGURE, capturedEventType)
        assertTrue(telemetryPropertiesSlot.isCaptured)
        val capturedProps = telemetryPropertiesSlot.captured
        assertNotNull(capturedProps)
        assertEquals(
            TelemetryUtility.formatConfigurationForTracking(ExponeaConfiguration(projectToken = "mock-token")),
            capturedProps
        )
    }

    @Test
    fun `should get current customer cookie after anonymize`() {
        initSdk()
        val cookie1 = Exponea.customerCookie
        assertNotNull(cookie1)
        Exponea.anonymize()
        val cookie2 = Exponea.customerCookie
        assertNotNull(cookie2)
        assertNotEquals(cookie1, cookie2)
    }

    @Test
    fun `should track telemetry for anonymize`() {
        initSdk()
        mockkConstructorFix(TelemetryManager::class)
        val telemetryTelemetryEventSlot = slot<com.exponea.sdk.telemetry.model.TelemetryEvent>()
        val telemetryPropertiesSlot = slot<MutableMap<String, String>>()
        every {
            anyConstructed<TelemetryManager>().reportEvent(
                capture(telemetryTelemetryEventSlot),
                capture(telemetryPropertiesSlot)
            )
        } just Runs
        Exponea.telemetry = TelemetryManager(ApplicationProvider.getApplicationContext())
        Exponea.anonymize()
        assertTrue(telemetryTelemetryEventSlot.isCaptured)
        val capturedEventType = telemetryTelemetryEventSlot.captured
        assertNotNull(capturedEventType)
        assertEquals(com.exponea.sdk.telemetry.model.TelemetryEvent.ANONYMIZE, capturedEventType)
        assertTrue(telemetryPropertiesSlot.isCaptured)
        val capturedProps = telemetryPropertiesSlot.captured
        assertNotNull(capturedProps)
        assertTrue(capturedProps.keys.contains("baseUrl"))
        assertTrue(capturedProps.keys.contains("projectToken"))
        assertTrue(capturedProps.keys.contains("authorization"))
    }

    @Test
    @Config(application = ReleaseMockApplication::class)
    fun `should have debug-off and safeMode-on in release build - before SDK init`() {
        assertFalse(Exponea.runDebugMode)
        assertTrue(Exponea.safeModeEnabled)
    }

    @Test
    @Config(application = ReleaseMockApplication::class)
    fun `should have debug-off and safeMode-on in release build after SDK init`() {
        initSdk()
        assertFalse(Exponea.runDebugMode)
        assertTrue(Exponea.safeModeEnabled)
    }

    @Test
    @Config(application = DebugMockApplication::class)
    fun `should have debug-off and safeMode-off in debug build before SDK init`() {
        // without context => false
        assertFalse(Exponea.runDebugMode)
        // without context => true
        assertTrue(Exponea.safeModeEnabled)
    }

    @Test
    @Config(application = DebugMockApplication::class)
    fun `should have debug-on and safeMode-off in debug build after SDK init`() {
        initSdk()
        assertTrue(Exponea.runDebugMode)
        assertFalse(Exponea.safeModeEnabled)
    }

    @Test
    fun `should have debug-on and safeMode-off in UnitTest run before SDK init`() {
        // without context => false
        assertFalse(Exponea.runDebugMode)
        // without context => true
        assertTrue(Exponea.safeModeEnabled)
    }

    @Test
    fun `should have debug-on and safeMode-off in UnitTest run after SDK init`() {
        initSdk()
        assertTrue(Exponea.runDebugMode)
        assertFalse(Exponea.safeModeEnabled)
    }

    @Test
    @Config(application = ReleaseMockApplication::class)
    fun `MODE behaviour as expected for release build before SDK init`() {
        val debugModeOverrideIndex = 0
        val safeModeOverrideIndex = 1
        val expectedDebugModeIndex = 2
        val expectedSafeModeIndex = 3
        val behaviorRules = listOf(
            arrayOf(null, null, false, true),
            arrayOf(null, true, false, true),
            arrayOf(null, false, false, false),
            arrayOf(true, null, true, true),
            arrayOf(true, true, true, true),
            arrayOf(true, false, true, false),
            arrayOf(false, null, false, true),
            arrayOf(false, true, false, true),
            arrayOf(false, false, false, false)
        )
        behaviorRules.forEach { env ->
            val debugOverride = env[debugModeOverrideIndex]
            val safeOverride = env[safeModeOverrideIndex]
            Exponea.runDebugModeOverride = debugOverride
            Exponea.safeModeOverride = safeOverride
            val expectedDebug = env[expectedDebugModeIndex]
            assertEquals(expectedDebug, Exponea.runDebugMode, """
            Debug should be '$expectedDebug' for state '$debugOverride':'$safeOverride'
            """.trimIndent())
            val expectedSafe = env[expectedSafeModeIndex]
            assertEquals(expectedSafe, Exponea.safeModeEnabled, """
            Expected to safeMode be '$expectedSafe' for state '$debugOverride':'$safeOverride'
            """.trimIndent())
        }
    }

    @Test
    @Config(application = DebugMockApplication::class)
    fun `MODE behaviour as expected for debug build before SDK init`() {
        val debugModeOverrideIndex = 0
        val safeModeOverrideIndex = 1
        val expectedDebugModeIndex = 2
        val expectedSafeModeIndex = 3
        val behaviorRules = listOf(
            arrayOf(null, null, false, true),
            arrayOf(null, true, false, true),
            arrayOf(null, false, false, false),
            arrayOf(true, null, true, true),
            arrayOf(true, true, true, true),
            arrayOf(true, false, true, false),
            arrayOf(false, null, false, true),
            arrayOf(false, true, false, true),
            arrayOf(false, false, false, false)
        )
        behaviorRules.forEach { env ->
            val debugOverride = env[debugModeOverrideIndex]
            val safeOverride = env[safeModeOverrideIndex]
            Exponea.runDebugModeOverride = debugOverride
            Exponea.safeModeOverride = safeOverride
            val expectedDebug = env[expectedDebugModeIndex]
            assertEquals(expectedDebug, Exponea.runDebugMode, """
            Debug should be '$expectedDebug' for state '$debugOverride':'$safeOverride'
            """.trimIndent())
            val expectedSafe = env[expectedSafeModeIndex]
            assertEquals(expectedSafe, Exponea.safeModeEnabled, """
            Expected to safeMode be '$expectedSafe' for state '$debugOverride':'$safeOverride'
            """.trimIndent())
        }
    }

    @Test
    @Config(application = ReleaseMockApplication::class)
    fun `MODE behaviour as expected for release build after SDK init`() {
        initSdk()
        val debugModeOverrideIndex = 0
        val safeModeOverrideIndex = 1
        val expectedDebugModeIndex = 2
        val expectedSafeModeIndex = 3
        val behaviorRules = listOf(
            arrayOf(null, null, false, true),
            arrayOf(null, true, false, true),
            arrayOf(null, false, false, false),
            arrayOf(true, null, true, false),
            arrayOf(true, true, true, true),
            arrayOf(true, false, true, false),
            arrayOf(false, null, false, true),
            arrayOf(false, true, false, true),
            arrayOf(false, false, false, false)
        )
        behaviorRules.forEach { env ->
            val debugOverride = env[debugModeOverrideIndex]
            val safeOverride = env[safeModeOverrideIndex]
            Exponea.runDebugModeOverride = debugOverride
            Exponea.safeModeOverride = safeOverride
            val expectedDebug = env[expectedDebugModeIndex]
            assertEquals(expectedDebug, Exponea.runDebugMode, """
            Debug should be '$expectedDebug' for state '$debugOverride':'$safeOverride'
            """.trimIndent())
            val expectedSafe = env[expectedSafeModeIndex]
            assertEquals(expectedSafe, Exponea.safeModeEnabled, """
            Expected to safeMode be '$expectedSafe' for state '$debugOverride':'$safeOverride'
            """.trimIndent())
        }
    }

    @Test
    @Config(application = DebugMockApplication::class)
    fun `MODE behaviour as expected for debug build after SDK init`() {
        initSdk()
        val debugModeOverrideIndex = 0
        val safeModeOverrideIndex = 1
        val expectedDebugModeIndex = 2
        val expectedSafeModeIndex = 3
        val behaviorRules = listOf(
            arrayOf(null, null, true, false),
            arrayOf(null, true, true, true),
            arrayOf(null, false, true, false),
            arrayOf(true, null, true, false),
            arrayOf(true, true, true, true),
            arrayOf(true, false, true, false),
            arrayOf(false, null, false, true),
            arrayOf(false, true, false, true),
            arrayOf(false, false, false, false)
        )
        behaviorRules.forEach { env ->
            val debugOverride = env[debugModeOverrideIndex]
            val safeOverride = env[safeModeOverrideIndex]
            Exponea.runDebugModeOverride = debugOverride
            Exponea.safeModeOverride = safeOverride
            val expectedDebug = env[expectedDebugModeIndex]
            assertEquals(expectedDebug, Exponea.runDebugMode, """
            Debug should be '$expectedDebug' for state '$debugOverride':'$safeOverride'
            """.trimIndent())
            val expectedSafe = env[expectedSafeModeIndex]
            assertEquals(expectedSafe, Exponea.safeModeEnabled, """
            Expected to safeMode be '$expectedSafe' for state '$debugOverride':'$safeOverride'
            """.trimIndent())
        }
    }

    @Test
    fun `should reset stopped flag after initialization`() {
        Exponea.isStopped = true
        initSdk()
        assertFalse(Exponea.isStopped)
        assertTrue(Exponea.isInitialized)
    }

    private fun initSdk() {
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), ExponeaConfiguration(projectToken = "mock-token"))
    }
}
