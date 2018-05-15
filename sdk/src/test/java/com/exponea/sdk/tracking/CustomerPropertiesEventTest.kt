package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.*
import com.exponea.sdk.repository.EventRepository
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CustomerPropertiesEventTest {

    private lateinit var repo: EventRepository

    @Before
    fun init() {
        ExponeaMockServer.setUp()

        val context = RuntimeEnvironment.application

        val configuration = ExponeaConfiguration()
        configuration.baseURL = ExponeaMockServer.address
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        configuration.maxTries = 10

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository

        // Clean event repository for testing purposes
        repo.clear()

    }


    @Test
    fun testEventTracked() {
        // Track event
        Exponea.updateCustomerProperties(
                customerIds = CustomerIds(cookie = "cookie"),
                properties = PropertiesList(hashMapOf("first_name" to "NewName"))
        )

        // Checking if event was successfully tracked
        assertEquals(1, repo.all().size)
    }

    @Test
    fun testEventSend() {

        // Track event
        Exponea.updateCustomerProperties(
                customerIds = CustomerIds(cookie = "cookie"),
                properties = PropertiesList(hashMapOf("first_name" to "NewName"))
        )

        ExponeaMockServer.setResponseSuccess("tracking/track_event_success.json")

        // Flush event and wait for result
        runBlocking {
            Exponea.flush()
            Exponea.component.flushManager.onFlushFinishListener = {

                // Checking that event was successfully sent
                assertEquals(0, repo.all().size)
            }
        }

        val result = ExponeaMockServer.getResult()
        // TODO Assert real value, wrong endpoint so far
        assertEquals("/track/v2/projects/projectToken/customers/events", result.path)
    }


}