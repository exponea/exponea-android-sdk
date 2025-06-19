package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.FetchManagerImpl
import com.exponea.sdk.manager.SegmentsManagerImpl
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.SegmentTest
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.repository.CustomerIdsRepositoryImpl
import com.exponea.sdk.telemetry.TelemetryManager
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaFetchSegmentsTest : ExponeaSDKTest() {

    internal val ACCEPTED_INIT_DURATION_MILLIS = 2000L

    @Before
    fun before() {
        mockkConstructorFix(FetchManagerImpl::class) {
            every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) }
        }
        every { anyConstructed<FetchManagerImpl>().linkCustomerIdsSync(any(), any()) } answers {
            Result<Any?>(true, null)
        }
        mockkConstructorFix(CustomerIdsRepositoryImpl::class)
        every { anyConstructed<CustomerIdsRepositoryImpl>().get() } returns SegmentTest.getCustomerIds()
        skipInstallEvent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            authorization = "Token mock-auth"
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
    }

    @Test
    fun `should fetch some segmentation data`() {
        every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        waitForIt(timeoutMS = SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + ACCEPTED_INIT_DURATION_MILLIS) { done ->
            Exponea.getSegments("discovery") { segments ->
                assertEquals(1, segments.size)
                assertEquals("mock-val", segments[0]["prop"])
                done()
            }
        }
    }

    @Test
    fun `should fetch no segmentation data for different category`() {
        every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        waitForIt(timeoutMS = SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + ACCEPTED_INIT_DURATION_MILLIS) { done ->
            Exponea.getSegments("content") { segments ->
                assertEquals(0, segments.size)
                done()
            }
        }
    }

    @Test
    fun `should track telemetry for segmentations fetch`() {
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
        every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        waitForIt(timeoutMS = SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + ACCEPTED_INIT_DURATION_MILLIS) { done ->
            Exponea.getSegments("content") { segments ->
                assertEquals(0, segments.size)
                done()
            }
        }
        assertTrue(telemetryTelemetryEventSlot.isCaptured)
        val capturedEventType = telemetryTelemetryEventSlot.captured
        assertNotNull(capturedEventType)
        assertEquals(com.exponea.sdk.telemetry.model.TelemetryEvent.RTS_GET_SEGMENTS, capturedEventType)
        assertTrue(telemetryPropertiesSlot.isCaptured)
        val capturedProps = telemetryPropertiesSlot.captured
        assertNotNull(capturedProps)
        assertEquals("content", capturedProps["exposingCategory"])
        assertEquals("false", capturedProps["forceFetch"])
    }

    @Test
    fun `should left no callback instance after successful get`() {
        every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        waitForIt(timeoutMS = SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + ACCEPTED_INIT_DURATION_MILLIS) { done ->
            Exponea.getSegments("discovery") { segments ->
                done()
            }
        }
        assertEquals(0, Exponea.segmentationDataCallbacks.size)
    }

    @Test
    fun `should left no callback instance after empty get`() {
        every { anyConstructed<FetchManagerImpl>().fetchSegments(any(), any(), any(), any()) } answers {
            arg<(Result<SegmentationCategories>) -> Unit>(2).invoke(Result(
                true,
                SegmentTest.buildSingleSegmentWithData(mapOf("prop" to "mock-val"))
            ))
        }
        waitForIt(timeoutMS = SegmentsManagerImpl.CHECK_DEBOUNCE_MILLIS + ACCEPTED_INIT_DURATION_MILLIS) { done ->
            Exponea.getSegments("content") { segments ->
                done()
            }
        }
        assertEquals(0, Exponea.segmentationDataCallbacks.size)
    }
}
