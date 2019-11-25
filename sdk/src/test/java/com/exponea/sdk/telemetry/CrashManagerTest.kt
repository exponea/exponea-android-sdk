package com.exponea.sdk.telemetry.model

import com.exponea.sdk.telemetry.CrashManager
import com.exponea.sdk.telemetry.storage.TelemetryStorage
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Date
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CrashManagerTest : ExponeaSDKTest() {
    private var savedCrashLogs = arrayListOf<CrashLog>()
    private lateinit var storage: TelemetryStorage
    private lateinit var upload: TelemetryUpload
    private lateinit var crashManager: CrashManager

    @Before
    fun before() {
        storage = mockk<TelemetryStorage> {
            every { saveCrashLog(any()) } just Runs
            every { deleteCrashLog(any()) } just Runs
            every { getAllCrashLogs() } returns savedCrashLogs
        }
        upload = mockk<TelemetryUpload>() {
            every { uploadCrashLog(any(), any()) } just Runs
        }
        crashManager = CrashManager(storage, upload, Date(), "mock-run-id")
    }

    @Test
    fun `should save unhandled exception`() {
        crashManager.start()
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(
            Thread.currentThread(),
            Exception("Mock exception")
        )
        verify(exactly = 1) { storage.saveCrashLog(any()) }
    }

    @Test
    fun `should not save unhandled exception unrelated to SDK`() {
        crashManager.start()
        val exception = Exception("Mock exception")
        exception.stackTrace = arrayOf()
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(
            Thread.currentThread(),
            exception
        )
        verify(exactly = 0) { storage.saveCrashLog(any()) }
    }

    @Test
    fun `should call original exception handler`() {
        var originalHandlerCalled = false
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> originalHandlerCalled = true }
        assertFalse(originalHandlerCalled)
        crashManager.start()
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(
            Thread.currentThread(),
            Exception("Mock exception")
        )
        assertTrue(originalHandlerCalled)
    }

    @Test
    fun `should upload crash logs`() {
        every { storage.getAllCrashLogs() } returns arrayListOf(
            CrashLog(Exception("mock exception 1"), Date(), "mock-run-id"),
            CrashLog(Exception("mock exception 2"), Date(), "mock-run-id"),
            CrashLog(Exception("mock exception 3"), Date(), "mock-run-id")
        )
        crashManager.start()
        verify(exactly = 3) { upload.uploadCrashLog(any(), any()) }
    }

    @Test
    fun `should delete crash log once uploaded`() {
        every { storage.getAllCrashLogs() } returns arrayListOf(
            CrashLog(Exception("mock exception"), Date(), "mock-run-id")
        )
        val callbackSlot = slot<(Result<Unit>) -> Unit>()
        every { upload.uploadCrashLog(any(), capture(callbackSlot)) } just Runs
        crashManager.start()
        verify(exactly = 1) { upload.uploadCrashLog(any(), any()) }
        verify(exactly = 0) { storage.deleteCrashLog(any()) }
        callbackSlot.captured(Result.success(Unit))
        verify(exactly = 1) { storage.deleteCrashLog(any()) }
    }

    @Test
    fun `should not delete crash log when upload fails`() {
        every { storage.getAllCrashLogs() } returns arrayListOf(
            CrashLog(Exception("mock exception"), Date(), "mock-run-id")
        )
        val callbackSlot = slot<(Result<Unit>) -> Unit>()
        every { upload.uploadCrashLog(any(), capture(callbackSlot)) } just Runs
        crashManager.start()
        verify(exactly = 1) { upload.uploadCrashLog(any(), any()) }
        verify(exactly = 0) { storage.deleteCrashLog(any()) }
        callbackSlot.captured(Result.failure(Exception()))
        verify(exactly = 0) { storage.deleteCrashLog(any()) }
    }

    @Test
    fun `should not crash`() {
        every {
            upload.uploadCrashLog(any(), any())
            storage.saveCrashLog(any())
            storage.deleteCrashLog(any())
            storage.getAllCrashLogs()
        } throws RuntimeException()
        crashManager.start()
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(
            Thread.currentThread(),
            Exception("Mock exception")
        )
    }
}