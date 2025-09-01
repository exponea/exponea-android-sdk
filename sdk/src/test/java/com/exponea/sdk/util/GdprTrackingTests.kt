package com.exponea.sdk.util

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class GdprTrackingTests {

    @Test
    fun `should tracked force for valid param`() {
        assertTrue { GdprTracking.isTrackForced("https://exponea.com/action?xnpe_force_track=true") }
    }

    @Test
    fun `should tracked force for VALID param`() {
        assertTrue { GdprTracking.isTrackForced("https://exponea.com/action?xnpe_force_track=TRUE") }
    }

    @Test
    fun `should tracked force for alternative param`() {
        assertTrue { GdprTracking.isTrackForced("https://exponea.com/action?xnpe_force_track=1") }
    }

    @Test
    fun `should tracked force for non valued param`() {
        assertTrue { GdprTracking.isTrackForced("https://exponea.com/action?xnpe_force_track") }
    }

    @Test
    fun `should not tracked force for no param`() {
        assertFalse { GdprTracking.isTrackForced("https://exponea.com/action") }
    }

    @Test
    fun `should not tracked force for false param`() {
        assertFalse { GdprTracking.isTrackForced("https://exponea.com/action?xnpe_force_track=false") }
    }

    @Test
    fun `should not tracked force for FALSE param`() {
        assertFalse { GdprTracking.isTrackForced("https://exponea.com/action?xnpe_force_track=FALSE") }
    }

    @Test
    fun `should not tracked force for alternative-false param`() {
        assertFalse { GdprTracking.isTrackForced("https://exponea.com/action?xnpe_force_track=0") }
    }

    @Test
    fun `should not tracked force for invalid param`() {
        assertFalse { GdprTracking.isTrackForced("https://exponea.com/action?xnpe_force_track=bull") }
    }

    @Test
    fun `should not tracked force for sub-string form elsewhere`() {
        assertFalse { GdprTracking.isTrackForced("https://exponea.com/action?key=xnpe_force_track") }
    }
}
