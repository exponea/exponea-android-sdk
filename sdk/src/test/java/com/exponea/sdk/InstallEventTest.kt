package com.exponea.sdk

import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import junit.framework.Assert.assertEquals
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


@RunWith(RobolectricTestRunner::class)
class InstallEventTest {
    companion object {
        var hasSetup: Boolean = false
    }

    val mockServer = MockWebServer()

    @Before
    public fun setup() {
        val mockServerAddress = setupWebServer()

        println("mockServerAddress: $mockServerAddress")

        val context = RuntimeEnvironment.application

        val configuration = ExponeaConfiguration()
        configuration.baseURL = mockServerAddress
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        Exponea.flushMode = FlushMode.MANUAL

        Exponea.init(context, configuration, null)

        hasSetup = true
    }

    @After
    public fun tearDown() {
        //mockServer.shutdown()
    }

    private fun setupWebServer(): String {
        mockServer.start()
        return mockServer.url("/").toString()
    }

    @Test
    fun sendInstallEvenTest_ShouldPass() {
        val mockResponse = MockResponse()
                .setBody("")
                .setResponseCode(200)

        mockServer.enqueue(mockResponse)

        Exponea.trackInstall()

        Exponea.flush()

        val request = mockServer.takeRequest()
        assertEquals("/track/v2/projects/projectToken/customers/events", request.path)
        assertEquals(request.getHeader("Authorization"), "projectAuthorization")
    }
}