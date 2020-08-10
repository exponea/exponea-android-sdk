package com.exponea.sdk.telemetry

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.model.EventLog
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.telemetry.upload.VSAppCenterTelemetryUpload
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import io.mockk.unmockkConstructor
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

    private val exceptedDevice = """{
        "appNamespace":"com.exponea.sdk.test",
        "appVersion":"com.exponea.sdk.test-unknown version",
        "appBuild":"0",
        "sdkName":"ExponeaSDK.android",
        "sdkVersion":"1.0.0",
        "osName":"Android",
        "osVersion":"8.1.0",
        "model":"robolectric",
        "locale":"en_US"
    }"""

    @Before
    fun before() {
        unmockkConstructor(VSAppCenterTelemetryUpload::class)
        server = ExponeaMockServer.createServer()
        upload = VSAppCenterTelemetryUpload(
            application = ApplicationProvider.getApplicationContext(),
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
    fun `should send session start`() {
        server.enqueue(MockResponse())
        upload.uploadSessionStart("mock-run-id") {}
        val request = server.takeRequest()
        // timestamp and id changes with every requests, let's check other properties
        val payload = JsonParser().parse(request.body.readString(Charset.defaultCharset()))
        val payloadEvent = payload.asJsonObject["logs"].asJsonArray[0].asJsonObject
        assertEquals("startSession", payloadEvent["type"].asString)
        assertEquals("mock-run-id", payloadEvent["sid"].asString)
        assertEquals(JsonParser().parse(exceptedDevice), payloadEvent["device"].asJsonObject)
    }

    private fun runUploadTest(
        makeRequest: (upload: TelemetryUpload, callback: (Result<Unit>) -> Unit) -> Unit,
        expectedRequest: String,
        requestPreprocessor: ((JsonElement) -> Unit)? = null
    ) {
        server.enqueue(MockResponse())
        waitForIt {
            makeRequest(upload) { result ->
                it.assertTrue(result.isSuccess)
                it()
            }
        }
        val request = server.takeRequest()
        assertEquals("mock-install-id", request.getHeader("Install-ID"))
        var jsonRequest = JsonParser().parse(request.body.readString(Charset.defaultCharset()))
        if (requestPreprocessor != null) requestPreprocessor(jsonRequest)
        assertEquals(
            JsonParser().parse(expectedRequest),
            jsonRequest
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
                    fatal = true,
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
                    "fatal":true,
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
    fun `should upload error data with logs to server`() {
        runUploadTest(
            { upload, callback ->
                val exception = Exception("Test exception")
                exception.stackTrace = arrayOf()
                val crashLog = CrashLog(
                    id = "ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    fatal = false,
                    errorData = TelemetryUtility.getErrorData(exception),
                    timestampMS = 1574155789000,
                    launchTimestampMS = 1573644923000,
                    runId = "mock-run-id",
                    logs = arrayListOf("log 1", "log 2")
                )
                upload.uploadCrashLog(crashLog, callback)
            },
            """ {"logs":[
                {
                    "type":"handledError",
                    "id":"ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    "sid":"mock-run-id",
                    "userId":"mock-user-id",
                    "device": $exceptedDevice,
                    "timestamp":"2019-11-19T09:29:49Z",
                    "fatal":false,
                    "exception": {
                        "type":"java.lang.Exception",
                        "message":"Test exception",
                        "frames":[],
                        "innerExceptions":[]
                    },
                    "appLaunchTimestamp":"2019-11-13T11:35:23Z",
                    "processId":0,
                    "processName":""
                },
                {
                    "type":"errorAttachment",
                    "id":"REPLACED ID FOR TEST",
                    "sid":"mock-run-id",
                    "userId":"mock-user-id",
                    "device": $exceptedDevice,
                    "timestamp":"2019-11-19T09:29:49Z",
                    "errorId":"ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    "contentType":"text/plain",
                    "data":"bG9nIDEKbG9nIDI="
                }
            ]}
            """,
            { request: JsonElement -> // replace the id that's generated with every request
                request
                    .asJsonObject["logs"]
                    .asJsonArray[1]
                    .asJsonObject
                    .add("id", JsonPrimitive("REPLACED ID FOR TEST"))
            }
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
        val response = MockResponse()
        response.setResponseCode(500)
        server.enqueue(response)
        val exception = Exception("Test exception")
        exception.stackTrace = arrayOf()
        waitForIt {
            upload.uploadCrashLog(CrashLog(exception, true, Date(), Date(), "mock-run-id")) { result ->
                it.assertTrue(result.isSuccess)
                it()
            }
        }
    }
}
