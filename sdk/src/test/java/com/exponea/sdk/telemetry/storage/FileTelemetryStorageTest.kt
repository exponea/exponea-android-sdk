package com.exponea.sdk.telemetry

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.storage.FileTelemetryStorage
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import java.io.File
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FileTelemetryStorageTest : ExponeaSDKTest() {
    @After
    fun after() {
        unmockkAll()
    }

    fun getMockCrashLog(): CrashLog = CrashLog(Exception("Boom!"), true, Date(), Date(), "mock-run-id")

    @Test
    fun `should return empty result without stored crash logs`() {
        val storage = FileTelemetryStorage(ApplicationProvider.getApplicationContext())
        assertEquals(emptyList(), storage.getAllCrashLogs())
    }

    @Test
    fun `should not fail deleting crash log that's not saved`() {
        val storage = FileTelemetryStorage(ApplicationProvider.getApplicationContext())
        storage.deleteCrashLog(getMockCrashLog())
    }

    @Test
    fun `should save-get-delete crash log`() {
        val storage = FileTelemetryStorage(ApplicationProvider.getApplicationContext())
        val crashLog = getMockCrashLog()
        storage.saveCrashLog(crashLog)
        val crashLogs = storage.getAllCrashLogs()
        assertEquals(1, crashLogs.size)
        assertEquals(crashLog, crashLogs[0])
        storage.deleteCrashLog(crashLog)
        assertEquals(emptyList(), storage.getAllCrashLogs())
    }

    @Test
    fun `should sort crash logs`() {
        val storage = FileTelemetryStorage(ApplicationProvider.getApplicationContext())
        val crashLog1 = getMockCrashLog()
        Thread.sleep(5)
        val crashLog2 = getMockCrashLog()
        Thread.sleep(5)
        val crashLog3 = getMockCrashLog()
        storage.saveCrashLog(crashLog2)
        storage.saveCrashLog(crashLog3)
        storage.saveCrashLog(crashLog1)
        val crashLogs = storage.getAllCrashLogs()
        assertEquals(3, crashLogs.size)
        assertTrue(crashLogs[0].timestampMS < crashLogs[1].timestampMS)
        assertTrue(crashLogs[1].timestampMS < crashLogs[2].timestampMS)
        storage.deleteCrashLog(crashLog1)
        storage.deleteCrashLog(crashLog2)
        storage.deleteCrashLog(crashLog3)
    }

    @Test
    fun `should not crash when unable to create storage folder`() {
        mockkConstructor(FileTelemetryStorage::class) // I'm not able to mock file, I'll mock this method
        every { anyConstructed<FileTelemetryStorage>()["getLogsDirectory"]() } returns null

        val storage = FileTelemetryStorage(ApplicationProvider.getApplicationContext())
        storage.saveCrashLog(getMockCrashLog())
        storage.deleteCrashLog(getMockCrashLog())
        assertEquals(arrayListOf(), storage.getAllCrashLogs())
    }

    @Test
    fun `should not crash when unable to creating storage folder throws`() {
        mockkConstructor(FileTelemetryStorage::class) // I'm not able to mock file, I'll mock this method
        every { anyConstructed<FileTelemetryStorage>()["getLogsDirectory"]() } throws RuntimeException()

        val storage = FileTelemetryStorage(ApplicationProvider.getApplicationContext())
        storage.saveCrashLog(getMockCrashLog())
        storage.deleteCrashLog(getMockCrashLog())
        assertEquals(arrayListOf(), storage.getAllCrashLogs())
    }

    @Test
    fun `should filter crash logs when loading files`() {
        val storage = FileTelemetryStorage(ApplicationProvider.getApplicationContext())
        storage.saveCrashLog(getMockCrashLog())
        val myDir = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, FileTelemetryStorage.DIRECTORY)
        File(myDir, "some_other_file").writeText("mock data")
        assertEquals(1, storage.getAllCrashLogs().size)
    }

    @Test
    fun `should skip and delete files with corrupt data when loading files`() {
        val storage = FileTelemetryStorage(ApplicationProvider.getApplicationContext())
        val crashLog1 = getMockCrashLog()
        storage.saveCrashLog(crashLog1)
        val crashLog2 = getMockCrashLog()
        storage.saveCrashLog(crashLog2)
        val myDir = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, FileTelemetryStorage.DIRECTORY)
        File(myDir, storage.getFileName(crashLog2)).writeText("{ corruptData:")
        assertEquals(1, storage.getAllCrashLogs().size)
        assertFalse(File(myDir, storage.getFileName(crashLog2)).exists())
    }
}
