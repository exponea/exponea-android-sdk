package com.exponea.sdk.telemetry.model

import com.exponea.sdk.testutil.ExponeaSDKTest
import java.util.Date
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CrashLogTest : ExponeaSDKTest() {
    @Test
    fun `should create crash log from exception`() {
        val e = Exception("Exception happened", Exception("Cause exception"))
        e.cause?.stackTrace = arrayOf(StackTraceElement("MockClass", "mockMethod", "mockFile", 123))
        e.stackTrace = arrayOf()
        val crashLog = CrashLog(e, true, Date(12345678), Date(12345), "mock-run-id")
        assertEquals("Exception happened", crashLog.errorData.message)
        assertEquals("Cause exception", crashLog.errorData.cause?.message)
        assertEquals(
            arrayListOf(ErrorStackTraceElement("MockClass", "mockMethod", "mockFile", 123)),
            crashLog.errorData.cause?.stackTrace
        )
        assertEquals(12345678, crashLog.timestampMS)
        assertEquals(12345, crashLog.launchTimestampMS)
        assertEquals("mock-run-id", crashLog.runId)
    }
}
