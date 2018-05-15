package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.*
import com.exponea.sdk.repository.EventRepository
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

}