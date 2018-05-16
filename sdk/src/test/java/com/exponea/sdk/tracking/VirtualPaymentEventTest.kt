package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PurchasedItem
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
        val payment = PurchasedItem(
                currency = "USD",
                value = 200.3,
                productId = "Item",
                productTitle = "Speed Boost", paymentSystem = "Sys")
        Exponea.trackPayment(CustomerIds(cookie = "cookie"), purchasedItem = payment)
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