package com.exponea.sdk.fetching

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockApi
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.*
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class FetchSegment {

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

        attributes.withProperty("first_name")
    }

    @After
    public fun tearDown() {
        //ExponeaMockServer.shutDown()
    }

    @Test
    fun getSegment_ShouldSuccess() {

        // Set the response to success and json result.
        ExponeaMockServer.setResponseSuccess("fetching/segmentation_success.json")

        var success: Boolean? = null
        var value: String? = null
        var error: FetchError? = null
        // Run blocking with coroutine to get the values from the async function.
        runBlocking {
            ExponeaMockApi.fetchCustomerId(attributes,
                    onSuccess = {
                        success = it.results.first().success
                        value = it.results.first().value
                    },
                    onFailure = {
                        success = it.success
                        error = it.results
                    }
            )
        }

        val request = ExponeaMockServer.getResult()

        // TODO assert real response
        assertEquals("/data/v2/projects/projectToken/customers/attributes", request.path)
        assertEquals(true, success)
        assertEquals("Segmentation", value)
    }

    @Test
    fun getSegment_ShouldFailure() {

        // Set the response to success and json result.
        ExponeaMockServer.setResponseError("fetching/segmentation_failure.json")

        var success: Boolean? = null
        var value: String? = null
        var error: FetchError? = null

        // Run blocking with coroutine to get the values from the async function.
        runBlocking {
            ExponeaMockApi.fetchCustomerId(attributes,
                    onSuccess = {
                        success = it.results.first().success
                        value = it.results.first().value
                    },
                    onFailure = {
                        error = it.results
                        success = it.success
                    }
            )
        }

        val request = ExponeaMockServer.getResult()

        // TODO assert real response
        assertEquals("/data/v2/projects/projectToken/customers/attributes", request.path)
        assertNotNull(error)
        assertEquals(null, value)
    }
}