package com.exponea.sdk.testutil

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.DeviceInitiatedRepositoryImpl
import org.junit.After
import java.util.concurrent.CountDownLatch

open class ExponeaSDKTest {
    companion object {
        @Synchronized fun waitUntilFlushed() {
            val lock = CountDownLatch(1)
            Exponea.component.flushManager.onFlushFinishListener = {
                lock.countDown()
            }
            if (Exponea.component.flushManager.isRunning) {
                lock.await()
            }
        }

        fun skipInstallEvent() {
            DeviceInitiatedRepositoryImpl(ExponeaPreferencesImpl(
                ApplicationProvider.getApplicationContext()
            )).set(true)
        }
    }

    @After
    fun resetExponea() {
        if (Exponea.isInitialized && Exponea.component.flushManager.isRunning) {
            // Database is shared between Exponea instances, FlushingManager is not
            // If flushing is ongoing after test is done, it can flush events from another test
            // You can use waitUntilFlushed() to fix this
            throw RuntimeException("Flushing still in progress after test is done!")
        }

        Exponea.reset()
    }
}