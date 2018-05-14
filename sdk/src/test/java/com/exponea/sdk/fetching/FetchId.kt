package com.exponea.sdk.fetching

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockApi
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.*
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class FetchId {

    val customerIds: CustomerIds = CustomerIds(registered = "rito@nodesagency.com")
    var attributes: CustomerAttributes = CustomerAttributes(customerIds)

    // Start the mockserver and set the exponea api configuration.
    @Before
    public fun setup() {

        ExponeaMockServer.setUp()

        val context = RuntimeEnvironment.application

        val configuration = ExponeaConfiguration()
        configuration.baseURL = ExponeaMockServer.address
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        Exponea.init(context, configuration)

        Exponea.flushMode = FlushMode.MANUAL

        attributes.withId("cookie")
    }

    @After
    public fun tearDown() {
        ExponeaMockServer.shutDown()
    }

    @Test
    fun getCustomerId_ShouldSuccess() {

        // Set the response to success and json result.
        ExponeaMockServer.setResponseSuccess("fetching/customer_id_success.json")

        var success: Boolean? = null
        var value: String? = null

        // Run blocking with coroutine to get the values from the async function.
        runBlocking {
            ExponeaMockApi.fetchCustomerId(attributes,
                    onSuccess = {
                        success = it.results.first().success
                        value = it.results.first().value
                    },
                    onFailure = {
                        success = false
                        value = null
                    }
            )
        }

        val request = ExponeaMockServer.getResult()

        assertEquals("/data/v2/projects/projectToken/customers/attributes", request.path)
        assertEquals(true, success)
        assertEquals("Marian", value)
    }

    @Test
    fun getCustomerId_ShouldFailure() {

        // Set the response to success and json result.
        ExponeaMockServer.setResponseError("fetching/customer_id_failure.json")

        var success: Boolean? = null
        var value: String? = null
        var error: String? = null

        // Run blocking with coroutine to get the values from the async function.
        runBlocking {
            ExponeaMockApi.fetchCustomerId(attributes,
                    onSuccess = {
                        success = it.results.first().success
                        value = it.results.first().value
                    },
                    onFailure = {
                        error = it
                        value = null
                    }
            )
        }

        val request = ExponeaMockServer.getResult()
        assertEquals("/data/v2/projects/projectToken/customers/attributes", request.path)
        assertNotNull(error)
        assertEquals(null, value)
    }
}