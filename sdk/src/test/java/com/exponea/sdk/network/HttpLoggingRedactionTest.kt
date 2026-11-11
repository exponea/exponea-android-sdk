package com.exponea.sdk.network

import com.exponea.sdk.models.ExponeaConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class HttpLoggingRedactionTest {

    private val token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjdXN0b21lciJ9.signature"
    private val logLines = mutableListOf<String>()
    private val logger = object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            logLines.add(message)
        }
    }
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setBody("{}"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun sendAuthorizedRequest(level: ExponeaConfiguration.HttpLoggingLevel) {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("Authorization", token).build())
            }
            .addInterceptor(NetworkHandlerImpl.createHttpLoggingInterceptor(level, logger))
            .build()
        val request = Request.Builder().url(server.url("/track")).build()
        client.newCall(request).execute().close()
    }

    @Test
    fun `should redact the Authorization header when logging headers`() {
        sendAuthorizedRequest(ExponeaConfiguration.HttpLoggingLevel.HEADERS)

        assertTrue(logLines.any { it.startsWith("Authorization:") })
        assertFalse(logLines.any { it.contains(token) })
    }

    @Test
    fun `should redact the Authorization header when logging bodies`() {
        sendAuthorizedRequest(ExponeaConfiguration.HttpLoggingLevel.BODY)

        assertTrue(logLines.any { it.startsWith("Authorization:") })
        assertFalse(logLines.any { it.contains(token) })
    }

    @Test
    fun `should still send the Authorization header to the server`() {
        sendAuthorizedRequest(ExponeaConfiguration.HttpLoggingLevel.BODY)

        assertEquals(token, server.takeRequest().getHeader("Authorization"))
    }
}
