package com.exponea.sdk.telemetry

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.model.EventLog
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.telemetry.upload.VSAppCenterTelemetryUpload
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import com.google.gson.JsonParser
import java.nio.charset.Charset
import java.util.Date
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class VSAppCenterTelemetryUploadTest : ExponeaSDKTest() {
    private lateinit var server: MockWebServer
    private lateinit var upload: VSAppCenterTelemetryUpload

    private val exceptedDevice = """{
        "appNamespace":"com.exponea.sdk.test",
        "appVersion":"unknown version",
        "appBuild":"unknown build",
        "sdkName":"ExponeaSDK.android",
        "sdkVersion":"1.0.0",
        "osName":"Android",
        "osVersion":"8.1.0",
        "model":"robolectric",
        "locale":"en_US"
    }"""

    @Before
    fun before() {
        server = MockWebServer()
    }

    private fun initializeUpload(): RecordedRequest {
        server.enqueue(MockResponse()) // for session start event
        upload = VSAppCenterTelemetryUpload(
            context = ApplicationProvider.getApplicationContext(),
            installId = "mock-install-id",
            sdkVersion = "1.0.0",
            userId = "mock-user-id",
            runId = "mock-run-id",
            uploadUrl = server.url("/something").toString()
        )
        return server.takeRequest() // session start
    }

    @After
    fun after() {
        server.shutdown()
    }

    @Test
    fun `should send session start when constructed`() {
        val request = initializeUpload()
        // timestamp and id changes with every requests, let's check other properties
        val payload = JsonParser().parse(request.body.readString(Charset.defaultCharset()))
        val payloadEvent = payload.asJsonObject["logs"].asJsonArray[0].asJsonObject
        assertEquals("startSession", payloadEvent["type"].asString)
        assertEquals("mock-run-id", payloadEvent["sid"].asString)
        assertEquals(JsonParser().parse(exceptedDevice), payloadEvent["device"].asJsonObject)
    }

    private fun runUploadTest(
        makeRequest: (upload: TelemetryUpload, callback: (Result<Unit>) -> Unit) -> Unit,
        expectedRequest: String
    ) {
        initializeUpload()
        server.enqueue(MockResponse())
        waitForIt {
            makeRequest(upload) { result ->
                it.assertTrue(result.isSuccess)
                it()
            }
        }
        val request = server.takeRequest()
        assertEquals("mock-install-id", request.getHeader("Install-ID"))
        assertEquals(
            JsonParser().parse(expectedRequest),
            JsonParser().parse(request.body.readString(Charset.defaultCharset()))
        )
    }

    @Test
    fun `should upload error data to server`() {
        runUploadTest(
            { upload, callback ->
                val exception = Exception("Test exception")
                exception.stackTrace = arrayOf(StackTraceElement("mock-class", "mock-method", "mock-file.java", 123))
                val crashLog = CrashLog(
                    id = "ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    fatal = false,
                    errorData = TelemetryUtility.getErrorData(exception),
                    timestampMS = 1574155789000,
                    launchTimestampMS = 1573644923000,
                    runId = "mock-run-id"
                )
                upload.uploadCrashLog(crashLog, callback)
            },
            """ {"logs":[
                {
                    "type":"managedError",
                    "id":"ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    "sid":"mock-run-id",
                    "userId":"mock-user-id",
                    "device": $exceptedDevice,
                    "timestamp":"2019-11-19T09:29:49Z",
                    "fatal":false,
                    "exception": {
                        "type":"java.lang.Exception",
                        "message":"Test exception",
                        "frames":[
                            {
                                "className":"mock-class",
                                "methodName":"mock-method",
                                "fileName":"mock-file.kt",
                                "lineNumber":123
                            }
                        ],
                        "innerExceptions":[]
                    },
                    "appLaunchTimestamp":"2019-11-13T11:35:23Z",
                    "processId":0,
                    "processName":""
                }
            ]}
            """
        )
    }

    @Test
    fun `should upload event data to server`() {
        runUploadTest(
            { upload, callback ->
                val eventLog = EventLog(
                    id = "ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    name = "mock-name",
                    timestampMS = 1574155789000,
                    properties = hashMapOf("mock-1" to "mock-value-1", "mock-2" to "mock-value-2"),
                    runId = "mock-run-id"
                )
                upload.uploadEventLog(eventLog, callback)
            },
            """ {"logs":[
                {
                    "type":"event",
                    "id":"ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    "sid":"mock-run-id",
                    "userId":"mock-user-id",
                    "device": $exceptedDevice,
                    "timestamp":"2019-11-19T09:29:49Z",
                    "name": "mock-name",
                    "properties": {
                        "mock-1": "mock-value-1",
                        "mock-2": "mock-value-2"
                    }
                }
            ]}
            """
        )
    }

    @Test
    fun `succeed if server returns 500`() {
        initializeUpload()
        val response = MockResponse()
        response.setResponseCode(500)
        server.enqueue(response)
        val exception = Exception("Test exception")
        exception.stackTrace = arrayOf()
        waitForIt {
            upload.uploadCrashLog(CrashLog(exception, true, Date(), "mock-run-id")) { result ->
                it.assertTrue(result.isSuccess)
                it()
            }
        }
    }
}