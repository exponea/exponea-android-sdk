package com.exponea.sdk.testutil

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.InAppContentBlocksManagerImpl
import com.exponea.sdk.manager.InAppMessageManagerImpl
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import com.exponea.sdk.telemetry.upload.VSAppCenterTelemetryUpload
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before

internal open class ExponeaSDKTest {
    companion object {
        fun skipInstallEvent() {
            DeviceInitiatedRepositoryImpl(ExponeaPreferencesImpl(
                ApplicationProvider.getApplicationContext()
            )).set(true)
        }
    }

    @Before
    fun disableTelemetry() {
        mockkConstructor(VSAppCenterTelemetryUpload::class)
        every { anyConstructed<VSAppCenterTelemetryUpload>().upload(any(), any()) } just Runs
    }

    @Before
    fun disableInAppMessagePrefetch() {
        mockkConstructor(InAppMessageManagerImpl::class)
        every { anyConstructed<InAppMessageManagerImpl>().preload() } just Runs
    }

    @Before
    fun disableInAppContentBlocksPrefetch() {
        mockkConstructor(InAppContentBlocksManagerImpl::class)
        every { anyConstructed<InAppContentBlocksManagerImpl>().loadInAppContentBlockPlaceholders() } just Runs
    }

    @After
    fun afterExponeaTest() {
        // we need to enforce the order here, first unmock, then resetExponea
        unmockAllSafely()
        resetExponea()
    }

    fun unmockAllSafely() {
        // mockk has a problem when it sometimes throws an exception, in that case just try again
        try { unmockkAll() } catch (error: ConcurrentModificationException) { unmockAllSafely() }
    }

    fun resetExponea() {
        if (Exponea.isInitialized && Exponea.componentForTesting.flushManager.isRunning) {
            // Database is shared between Exponea instances, FlushingManager is not
            // If flushing is ongoing after test is done, it can flush events from another test
            // You can use waitUntilFlushed() to fix this
            throw RuntimeException("Flushing still in progress after test is done!")
        }

        Exponea.reset()
    }
}
