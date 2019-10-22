package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PurchasedItem
import com.exponea.sdk.testutil.ExponeaSDKTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
internal class VirtualPaymentEventTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()
        lateinit var server: MockWebServer
        val payment = PurchasedItem(
                currency = "USD",
                value = 200.3,
                productId = "Item",
                productTitle = "Speed Boost",
                paymentSystem = "Sys"
        )

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = MockWebServer()
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 10
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    @Before
    fun prepareForTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        skipInstallEvent()
        Exponea.init(context, configuration)
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL
    }

    @Test
    fun sendPaymentEvent_ShouldPass() {

        // Set the response to success and json result.
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        Exponea.trackPaymentEvent(
                purchasedItem = payment
        )
        Exponea.flushData()
        waitUntilFlushed()

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
        assertEquals(request.getHeader("Authorization"), "TestTokenAuthentication")
    }

}
