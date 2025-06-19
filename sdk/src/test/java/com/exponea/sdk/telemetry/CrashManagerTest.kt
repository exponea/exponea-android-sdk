package com.exponea.sdk.telemetry.model

import com.exponea.sdk.telemetry.CrashManager
import com.exponea.sdk.telemetry.storage.TelemetryStorage
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import java.lang.System.currentTimeMillis
import java.util.Calendar
import java.util.Date
import kotlin.concurrent.thread
import kotlin.test.assertEquals
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
    fun `should upload handled exception`() {
        every { upload.uploadCrashLog(any(), any()) } answers {
            secondArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }
        crashManager.handleException(Exception("Boom!"), false, Thread.currentThread())
        verify(exactly = 1) { upload.uploadCrashLog(any(), any()) }
        verify(exactly = 0) { storage.saveCrashLog(any()) }
    }

    @Test
    fun `should save handled exception if upload fails`() {
        every { upload.uploadCrashLog(any(), any()) } answers {
            secondArg<(Result<Unit>) -> Unit>().invoke(Result.failure(Exception("Upload failed")))
        }
        crashManager.handleException(Exception("Boom!"), false, Thread.currentThread())
        verify(exactly = 1) {
            upload.uploadCrashLog(any(), any())
            storage.saveCrashLog(any())
        }
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
            CrashLog(
                Exception("mock exception 1"),
                true,
                Date(),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            ),
            CrashLog(
                Exception("mock exception 2"),
                true,
                Date(),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            ),
            CrashLog(
                Exception("mock exception 3"),
                true,
                Date(),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            )
        )
        crashManager.start()
        verify(exactly = 3) { upload.uploadCrashLog(any(), any()) }
    }

    @Test
    fun `should delete crash log once uploaded`() {
        every { storage.getAllCrashLogs() } returns arrayListOf(
            CrashLog(
                Exception("mock exception"),
                true,
                Date(),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            )
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
            CrashLog(
                Exception("mock exception"),
                true,
                Date(),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            )
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
    fun `should delete crashlogs older than log retention instead of uploading`() {
        val dateDaysAgo = { days: Int ->
            Calendar.getInstance().run {
                add(Calendar.DAY_OF_YEAR, -days)
                time
            }
        }
        every { storage.getAllCrashLogs() } returns arrayListOf(
            CrashLog(
                Exception("mock exception 1"),
                true,
                dateDaysAgo(10),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            ),
            CrashLog(
                Exception("mock exception 2"),
                true,
                dateDaysAgo(0),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            ),
            CrashLog(
                Exception("mock exception 3"),
                true,
                dateDaysAgo(20),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            ),
            CrashLog(
                Exception("mock exception 4"),
                true,
                dateDaysAgo(14),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            ),
            CrashLog(
                Exception("mock exception 5"),
                true,
                dateDaysAgo(16),
                Date(),
                "mock-run-id",
                emptyList(),
                Thread.currentThread()
            )
        )
        crashManager.start()
        verifySequence {
            storage.getAllCrashLogs()
            upload.uploadCrashLog(any(), any())
            upload.uploadCrashLog(any(), any())
            storage.deleteCrashLog(any())
            upload.uploadCrashLog(any(), any())
            storage.deleteCrashLog(any())
        }
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

    @Test
    fun `should add log messages to caught exceptions`() {
        val timestamp = currentTimeMillis()
        val crashLogSlot = slot<CrashLog>()
        every { upload.uploadCrashLog(capture(crashLogSlot), any()) } just Runs
        crashManager.saveLogMessage(this, "message", timestamp)
        crashManager.handleException(Exception("message"), false, Thread.currentThread())
        assertEquals(1, crashLogSlot.captured.logs?.size)
        assertEquals("${Date(timestamp)} CrashManagerTest: message", crashLogSlot.captured.logs?.get(0))
    }

    @Test
    fun `should add log messages to crashes`() {
        val timestamp = currentTimeMillis()
        val crashLogSlot = slot<CrashLog>()
        every { storage.saveCrashLog(capture(crashLogSlot)) } just Runs
        crashManager.saveLogMessage(this, "message", timestamp)
        crashManager.start()
        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(
            Thread.currentThread(),
            Exception("Mock exception")
        )
        assertEquals(1, crashLogSlot.captured.logs?.size)
        assertEquals("${Date(timestamp)} CrashManagerTest: message", crashLogSlot.captured.logs?.get(0))
    }

    @Test
    fun `should limit log messages added to crashes`() {
        val timestamp = currentTimeMillis()
        val crashLogSlot = slot<CrashLog>()
        every { upload.uploadCrashLog(capture(crashLogSlot), any()) } just Runs
        for (i in 1..1000) crashManager.saveLogMessage(this, "message $i", timestamp)
        crashManager.handleException(Exception("message"), false, Thread.currentThread())
        assertEquals(100, crashLogSlot.captured.logs?.size)
        for (i in 0..99) {
            assertEquals("${Date(timestamp)} CrashManagerTest: message ${1000 - i}", crashLogSlot.captured.logs?.get(i))
        }
    }

    @Test
    fun `should save log messages from multiple threads`() {
        waitForIt {
            val threadCount = 10
            var done = 0
            for (i in 0..threadCount) {
                thread {
                    for (x in 0..100) crashManager.saveLogMessage(this, "message", currentTimeMillis())
                    done++
                    if (done == threadCount) it()
                }
            }
        }
    }
}
