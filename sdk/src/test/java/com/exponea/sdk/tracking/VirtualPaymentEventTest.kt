package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockApi
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.*
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class VirtualPaymentEventTest {

    companion object {
        val configuration = ExponeaConfiguration()
        val server = MockWebServer()
        val payment = PurchasedItem(
                currency = "USD",
                value = 200.3,
                productId = "Item",
                productTitle = "Speed Boost",
                paymentSystem = "Sys"
        )

        @BeforeClass @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestBasicAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 10
        }

        @AfterClass
        fun tearDown() {
            server.shutdown()
        }
    }

    @Before
    fun prepareForTest() {

        val context = RuntimeEnvironment.application

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL
    }

    @Test
    fun sendPaymentEvent_ShouldPass() {

        // Set the response to success and json result.
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        Exponea.trackPaymentEvent(
                purchasedItem = payment
        )

        // Run blocking with coroutine to get the values from the async function.
        runBlocking {
            ExponeaMockApi.flush()
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
        assertEquals(request.getHeader("Authorization"), "TestBasicAuthentication")
    }

}