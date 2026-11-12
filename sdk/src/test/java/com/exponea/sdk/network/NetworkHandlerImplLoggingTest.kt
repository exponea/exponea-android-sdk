package com.exponea.sdk.network

import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.network.auth.AuthStrategy
import com.exponea.sdk.testutil.ExponeaMockServer
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class NetworkHandlerImplLoggingTest {

    private val authorizationHeaderName = "Authorization"
    private val authorizationValue = "Bearer secret-jwt-token"
    private val customHeaderName = "X-Custom"
    private val customHeaderValue = "visible"
    private val requestPath = "/test"
    private val requestBody = "{\"visible\":\"body\"}"
    private val responseBody = "{}"
    private lateinit var server: MockWebServer
    private lateinit var logLines: MutableList<String>

    private val loggedText: String
        get() = logLines.joinToString("\n")

    @Before
    fun setup() {
        server = ExponeaMockServer.createServer(MockResponse().setResponseCode(200).setBody(responseBody))
        logLines = mutableListOf()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun executeRequest(httpLoggingLevel: ExponeaConfiguration.HttpLoggingLevel) {
        val handler = NetworkHandlerImpl(
            exponeaConfiguration = ExponeaConfiguration().apply { this.httpLoggingLevel = httpLoggingLevel },
            authTokenRepository = mockk(relaxed = true),
            customerIdsRepository = mockk(relaxed = true),
            legacyAuthorizationProvider = null,
            httpLogger = { logLines.add(it) },
            authCallbackProvider = { null }
        )
        handler.post(
            server.url(requestPath).toString(),
            AuthStrategy.PublicApiKey(authorizationValue),
            requestBody,
            mapOf(customHeaderName to customHeaderValue)
        ).execute().close()
    }

    private fun assertAuthorizationRedacted() {
        assertThat(loggedText, not(containsString(authorizationValue)))
        assertThat(logLines, hasItem(startsWith("$authorizationHeaderName:")))
    }

    @Test
    fun `should redact Authorization header at HEADERS level`() {
        executeRequest(ExponeaConfiguration.HttpLoggingLevel.HEADERS)

        assertAuthorizationRedacted()
    }

    @Test
    fun `should redact Authorization header at BODY level`() {
        executeRequest(ExponeaConfiguration.HttpLoggingLevel.BODY)

        assertAuthorizationRedacted()
    }

    @Test
    fun `should still send the real Authorization header`() {
        executeRequest(ExponeaConfiguration.HttpLoggingLevel.BODY)

        assertThat(server.takeRequest().getHeader(authorizationHeaderName), equalTo(authorizationValue))
    }

    @Test
    fun `should keep non-sensitive headers in logs`() {
        executeRequest(ExponeaConfiguration.HttpLoggingLevel.HEADERS)

        assertThat(logLines, hasItem("$customHeaderName: $customHeaderValue"))
    }

    @Test
    fun `should still log body at BODY level`() {
        executeRequest(ExponeaConfiguration.HttpLoggingLevel.BODY)

        assertThat(loggedText, containsString(requestBody))
    }

    @Test
    fun `should not log anything at NONE level`() {
        executeRequest(ExponeaConfiguration.HttpLoggingLevel.NONE)

        assertThat(logLines, empty())
    }

    @Test
    fun `should not log headers at BASIC level`() {
        executeRequest(ExponeaConfiguration.HttpLoggingLevel.BASIC)

        assertThat(logLines, not(empty()))
        assertThat(loggedText, not(containsString(authorizationHeaderName)))
    }
}
