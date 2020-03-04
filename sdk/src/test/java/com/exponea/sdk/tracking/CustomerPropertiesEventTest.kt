package com.exponea.sdk.tracking

import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.waitForIt
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CustomerPropertiesEventTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration(automaticSessionTracking = false)
        val customerIds = CustomerIds().withId("registered", "john@doe.com")
        val properties = PropertiesList(hashMapOf("first_name" to "NewName"))
        lateinit var server: MockWebServer

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = ExponeaMockServer.createServer()
            configuration.projectToken = "TestTokem"
            configuration.authorization = "Token TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")

            configuration.maxTries = 10
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    private lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(ApplicationProvider.getApplicationContext(), configuration)

        repo = Exponea.componentForTesting.eventRepository
    }

    @Test
    fun testEventTracked_ShouldSuccess() {
        // Track event
        Exponea.identifyCustomer(
                customerIds = customerIds,
                properties = properties
        )

        // Checking if event was successfully tracked
        assertEquals(1, repo.all().size)
    }

    @Test
    fun testEventSend_ShouldSuccess() {
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        // Track event
        Exponea.identifyCustomer(
                customerIds = customerIds,
                properties = properties
        )

        waitForIt {
            Exponea.componentForTesting.flushManager.flushData { _ ->
                assertEquals(0, repo.all().size)
                it()
            }
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/track/v2/projects/TestTokem/customers", request.path)
    }
}
