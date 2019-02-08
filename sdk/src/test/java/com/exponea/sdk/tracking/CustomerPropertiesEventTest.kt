package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.EventRepository
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CustomerPropertiesEventTest {

    companion object {
        val configuration = ExponeaConfiguration()
        val customerIds = CustomerIds().withId("registered", "john@doe.com")
        val properties = PropertiesList(hashMapOf("first_name" to "NewName"))
        val server = MockWebServer()

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")

            configuration.maxTries = 10
        }

        @AfterClass
        fun tearDown() {
            server.shutdown()
        }
    }

    private lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {
        ExponeaMockServer.setResponseSuccess(EventTrackTest.server, "tracking/track_event_success.json")

        val context = RuntimeEnvironment.application

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository

        // Clean event repository for testing purposes
        repo.clear()
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
    fun testEventSend_ShoudSuccess() {

        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        // Track event
        Exponea.identifyCustomer(
                customerIds = customerIds,
                properties = properties
        )

        Exponea.flushData()

        val syncObject = Object()

        Exponea.component.flushManager.onFlushFinishListener = {
            // Checking that event was successfully sent
            assertEquals(0, repo.all().size)
            synchronized(syncObject) { syncObject.notify() }
        }
        // Flush event and wait for result
        Exponea.component.flushManager.flushData()

        synchronized(syncObject) { syncObject.wait() }

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/track/v2/projects/TestTokem/customers", request.path)
    }
}