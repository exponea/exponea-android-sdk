package com.exponea.sdk

import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.Payment
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class VirtualPaymentEventTest {

    val mockServer = MockWebServer()

    @Before
    fun setup() {
        val mockServerAddress = setupWebServer()

        println("mockServerAddress: $mockServerAddress")

        val context = RuntimeEnvironment.application

        val configuration = ExponeaConfiguration()
        configuration.baseURL = mockServerAddress
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        Exponea.init(context, configuration)

        Exponea.flushMode = FlushMode.MANUAL

    }

    @Test
    fun sendPaymentEvent_ShouldPass() {
        val mockResponse = MockResponse()
                .setBody("")
                .setResponseCode(200)

        mockServer.enqueue(mockResponse)
        val payment = Payment("USD", 200.3, "Item", "Type")
        Exponea.trackPayment(CustomerIds(cookie = "cookie"), payment = payment)
        Exponea.flush()

        val request = mockServer.takeRequest()
        assertEquals("/track/v2/projects/projectToken/customers/events", request.path)
        assertEquals(request.getHeader("Authorization"), "projectAuthorization")

    }


    private fun setupWebServer(): String {
        mockServer.start()
        return mockServer.url("/").toString()
    }
}