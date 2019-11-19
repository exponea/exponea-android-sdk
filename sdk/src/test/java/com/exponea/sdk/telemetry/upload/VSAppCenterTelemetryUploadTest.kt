package com.exponea.sdk.telemetry

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.upload.VSAppCenterAPIRequestData
import com.exponea.sdk.telemetry.upload.VSAppCenterTelemetryUpload
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import com.google.gson.Gson
import java.nio.charset.Charset
import java.util.Date
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VSAppCenterTelemetryUploadTest : ExponeaSDKTest() {
    private lateinit var server: MockWebServer
    private lateinit var upload: VSAppCenterTelemetryUpload

    @Before
    fun before() {
        server = MockWebServer()
        upload = VSAppCenterTelemetryUpload(
            context = ApplicationProvider.getApplicationContext(),
            installId = "mock-install-id",
            sdkVersion = "1.0.0",
            userId = "mock-user-id",
            uploadUrl = server.url("/something").toString()
        )
    }

    @After
    fun after() {
        server.shutdown()
    }

    @Test
    fun `should send request to server`() {
        server.enqueue(MockResponse())
        val exception = Exception("Test exception")
        exception.stackTrace = arrayOf()
        waitForIt {
            upload.uploadCrashLog(CrashLog(exception, Date(1573644923000))) { result ->
                it.assertTrue(result.isSuccess)
                it()
            }
        }
        val request = server.takeRequest()
        assertEquals("mock-install-id", request.getHeader("Install-ID"))
        val payload = Gson().fromJson(
            request.body.readString(Charset.defaultCharset()),
            VSAppCenterAPIRequestData::class.java
        )
        assertEquals("2019-11-13T11:35:23Z", payload.logs[0].appLaunchTimestamp)
        assertEquals("Test exception", payload.logs[0].exception.message)
    }

    @Test
    fun `succeed if server returns 500`() {
        val response = MockResponse()
        response.setResponseCode(500)
        server.enqueue(response)
        val exception = Exception("Test exception")
        exception.stackTrace = arrayOf()
        waitForIt {
            upload.uploadCrashLog(CrashLog(exception, Date())) { result ->
                it.assertTrue(result.isSuccess)
                it()
            }
        }
    }
}