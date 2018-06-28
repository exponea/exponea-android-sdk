package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockApi
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.*
import com.exponea.sdk.repository.EventRepository
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
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

    private lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {

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

        // Track event
        Exponea.identifyCustomer(
                customerIds = customerIds,
                properties = properties
        )

        ExponeaMockServer.setResponseSuccess(server,"tracking/track_event_success.json")
        Exponea.flushData()

        // Flush event and wait for result
        runBlocking {
            ExponeaMockApi.flush()
            Exponea.component.flushManager.onFlushFinishListener = {
                // Checking that event was successfully sent
                assertEquals(0, repo.all().size)
            }
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/track/v2/projects/TestTokem/customers", request.path)
    }
}