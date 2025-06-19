package com.exponea.sdk.telemetry

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.telemetry.model.CrashLog
import com.exponea.sdk.telemetry.model.ErrorStackTraceElement
import com.exponea.sdk.telemetry.model.EventLog
import com.exponea.sdk.telemetry.model.ThreadInfo
import com.exponea.sdk.telemetry.upload.SentryTelemetryUpload
import com.exponea.sdk.telemetry.upload.TelemetryUpload
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import io.mockk.every
import io.mockk.mockkConstructor
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
internal class SentryTelemetryUploadTest : ExponeaSDKTest() {
    private lateinit var server: MockWebServer
    private lateinit var upload: SentryTelemetryUpload

    @Before
    fun before() {
        // this is mocked by default in ExponeaSDKTest
        unmockkConstructor(SentryTelemetryUpload::class)
        server = ExponeaMockServer.createServer()
        upload = SentryTelemetryUpload(
            application = ApplicationProvider.getApplicationContext(),
            installId = "mock-install-id"
        ).apply {
            sentryEnvelopeApiUrl = server.url("/something").toString()
        }
    }

    @After
    fun after() {
        server.shutdown()
    }

    @Test
    fun `should send session start`() {
        mockkConstructor(Date::class)
        every { anyConstructed<Date>().time } returns 1_600_000_000_000L
        try {
            runUploadTest(
                { upload, callback ->
                    upload.uploadSessionStart("mock-run-id", callback)
                },
                """
                {"dsn":"https://0c1ab20fe28a048ab96370522875d4f6@msdk.bloomreach.co/10","sent_at":"2020-09-13T12:26:40Z"}
                {"type":"session","length":637,"content_type":"application/json"}
                {"started":"2020-09-13T12:26:40Z","timestamp":"2020-09-13T12:26:40Z","did":"mock-run-id","sid":"mock-run-id","init":true,"status":"ok","seq":1600000000000,"attrs":{"release":"com.exponea.sdk.test@unknown version - SDK 4.4.0","environment":"debug"},"extra":{"uuid":"mock-install-id","projectToken":"","sdkVersion":"4.4.0","sdkName":"ExponeaSDK.android","appName":"com.exponea.sdk.test","appVersion":"unknown version","appBuild":"0","appIdentifier":"com.exponea.sdk.test","osName":"Android","osVersion":"15","deviceModel":"robolectric","deviceManufacturer":"robolectric","brand":"robolectric","locale":"en_US","eventName":"sentrySession"}}
            """.trimIndent()
            )
        } finally {
            unmockkConstructor(Date::class)
        }
    }

    private fun runUploadTest(
        makeRequest: (upload: TelemetryUpload, callback: (Result<Unit>) -> Unit) -> Unit,
        expectedRequest: String
    ) {
        server.enqueue(MockResponse())
        waitForIt {
            makeRequest(upload) { result ->
                it.assertTrue(result.isSuccess)
                it()
            }
        }
        val request = server.takeRequest()
        val requestBody = request.body.readString(Charset.defaultCharset())
        assertEquals(expectedRequest, requestBody)
    }

    @Test
    fun `should upload error data to server`() {
        val dollar = "$"
        runUploadTest(
            { upload, callback ->
                val exception = Exception("Test exception")
                exception.stackTrace = arrayOf(StackTraceElement("mock-class", "mock-method", "mock-file.java", 123))
                val crashLog = CrashLog(
                    id = "ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    errorData = TelemetryUtility.getErrorData(exception),
                    fatal = true,
                    timestampMS = 1574155789000,
                    launchTimestampMS = 1573644923000,
                    runId = "mock-run-id",
                    logs = null,
                    thread = buildThreadInfoSample()
                )
                upload.uploadCrashLog(crashLog, callback)
            },
            """
                {"event_id":"ca46cb383c0f46fb91ef5c5345619af7","dsn":"https://0c1ab20fe28a048ab96370522875d4f6@msdk.bloomreach.co/10","sent_at":"2019-11-19T09:29:49Z"}
                {"type":"event","length":1439,"content_type":"application/json"}
                {"timestamp":"2019-11-19T09:29:49Z","logger":"errorLogger","threads":{"values":[{"id":19,"name":"SDK 35 Main Thread","state":"RUNNABLE","crashed":true,"current":true,"daemon":false,"main":true,"stacktrace":{"frames":[{"filename":"Thread.java","function":"run","module":"Thread","lineno":840}]}}]},"exception":{"values":[{"type":"java.lang.Exception","value":"Test exception","stacktrace":{"frames":[{"filename":"mock-file.java","function":"mock-method","module":"mock-class","lineno":123}]},"mechanism":{"type":"generic","description":"generic","handled":false},"thread_id":19}]},"level":"fatal","fingerprint":["error"],"event_id":"ca46cb383c0f46fb91ef5c5345619af7","contexts":{"app":{"type":"app","app_identifier":"com.exponea.sdk.test","app_name":"com.exponea.sdk.test","app_build":"0"},"device":{"type":"device","model":"robolectric","manufacturer":"robolectric","brand":"robolectric"},"os":{"type":"os","name":"Android","version":"15"}},"tags":{"uuid":"mock-install-id","projectToken":"","sdkVersion":"4.4.0","sdkName":"ExponeaSDK.android","appName":"com.exponea.sdk.test","appVersion":"unknown version","appBuild":"0","appIdentifier":"com.exponea.sdk.test","osName":"Android","osVersion":"15","deviceModel":"robolectric","deviceManufacturer":"robolectric","brand":"robolectric","locale":"en_US","eventName":"sentryError"},"release":"com.exponea.sdk.test@unknown version - SDK 4.4.0","environment":"debug","platform":"java","extra":{}}
            """.trimIndent()
        )
    }

    @Test
    fun `should upload error data with logs to server`() {
        val dollar = "$"
        runUploadTest(
            { upload, callback ->
                val exception = Exception("Test exception")
                exception.stackTrace = arrayOf()
                val crashLog = CrashLog(
                    id = "ca46cb38-3c0f-46fb-91ef-5c5345619af7",
                    errorData = TelemetryUtility.getErrorData(exception),
                    fatal = false,
                    timestampMS = 1574155789000,
                    launchTimestampMS = 1573644923000,
                    runId = "mock-run-id",
                    logs = arrayListOf("log 1", "log 2"),
                    thread = buildThreadInfoSample()
                )
                upload.uploadCrashLog(crashLog, callback)
            },
            """
                {"event_id":"ca46cb383c0f46fb91ef5c5345619af7","dsn":"https://0c1ab20fe28a048ab96370522875d4f6@msdk.bloomreach.co/10","sent_at":"2019-11-19T09:29:49Z"}
                {"type":"event","length":1350,"content_type":"application/json"}
                {"timestamp":"2019-11-19T09:29:49Z","logger":"errorLogger","threads":{"values":[{"id":19,"name":"SDK 35 Main Thread","state":"RUNNABLE","crashed":false,"current":true,"daemon":false,"main":true,"stacktrace":{"frames":[{"filename":"Thread.java","function":"run","module":"Thread","lineno":840}]}}]},"exception":{"values":[{"type":"java.lang.Exception","value":"Test exception","stacktrace":{"frames":[]},"mechanism":{"type":"generic","description":"generic","handled":true},"thread_id":19}]},"level":"error","fingerprint":["error"],"event_id":"ca46cb383c0f46fb91ef5c5345619af7","contexts":{"app":{"type":"app","app_identifier":"com.exponea.sdk.test","app_name":"com.exponea.sdk.test","app_build":"0"},"device":{"type":"device","model":"robolectric","manufacturer":"robolectric","brand":"robolectric"},"os":{"type":"os","name":"Android","version":"15"}},"tags":{"uuid":"mock-install-id","projectToken":"","sdkVersion":"4.4.0","sdkName":"ExponeaSDK.android","appName":"com.exponea.sdk.test","appVersion":"unknown version","appBuild":"0","appIdentifier":"com.exponea.sdk.test","osName":"Android","osVersion":"15","deviceModel":"robolectric","deviceManufacturer":"robolectric","brand":"robolectric","locale":"en_US","eventName":"sentryError"},"release":"com.exponea.sdk.test@unknown version - SDK 4.4.0","environment":"debug","platform":"java","extra":{}}
            """.trimIndent()
        )
    }

    private fun buildThreadInfoSample() = ThreadInfo(
        id = 19,
        name = "SDK 35 Main Thread",
        state = "RUNNABLE",
        isDaemon = false,
        isCurrent = true,
        isMain = true,
        stackTrace = listOf(ErrorStackTraceElement(
            className = "Thread",
            methodName = "run",
            fileName = "Thread.java",
            lineNumber = 840
        ))
    )

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
            """
                {"event_id":"ca46cb383c0f46fb91ef5c5345619af7","dsn":"https://0c1ab20fe28a048ab96370522875d4f6@msdk.bloomreach.co/10","sent_at":"2019-11-19T09:29:49Z"}
                {"type":"event","length":1002,"content_type":"application/json"}
                {"timestamp":"2019-11-19T09:29:49Z","message":{"formatted":"mock-name"},"logger":"messageLogger","level":"info","fingerprint":["metrics"],"event_id":"ca46cb383c0f46fb91ef5c5345619af7","contexts":{"app":{"type":"app","app_identifier":"com.exponea.sdk.test","app_name":"com.exponea.sdk.test","app_build":"0"},"device":{"type":"device","model":"robolectric","manufacturer":"robolectric","brand":"robolectric"},"os":{"type":"os","name":"Android","version":"15"}},"tags":{"uuid":"mock-install-id","projectToken":"","sdkVersion":"4.4.0","sdkName":"ExponeaSDK.android","appName":"com.exponea.sdk.test","appVersion":"unknown version","appBuild":"0","appIdentifier":"com.exponea.sdk.test","osName":"Android","osVersion":"15","deviceModel":"robolectric","deviceManufacturer":"robolectric","brand":"robolectric","locale":"en_US","eventName":"mock-name"},"release":"com.exponea.sdk.test@unknown version - SDK 4.4.0","environment":"debug","platform":"java","extra":{"mock-2":"mock-value-2","mock-1":"mock-value-1"}}
            """.trimIndent()
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
            upload.uploadCrashLog(
                CrashLog(exception, true, Date(), Date(), "mock-run-id", null, Thread.currentThread())
            ) { result ->
                it.assertTrue(result.isSuccess)
                it()
            }
        }
    }
}
