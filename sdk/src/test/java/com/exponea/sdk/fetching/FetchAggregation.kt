package com.exponea.sdk.fetching

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockApi
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.CustomerAttributes
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class FetchAggregation {


    @Before
    fun init() {
        ExponeaMockServer.setUp()

        val context = RuntimeEnvironment.application

        val configuration = ExponeaConfiguration()
        configuration.baseURL = ExponeaMockServer.address
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        Exponea.init(context, configuration)

        Exponea.flushMode = FlushMode.MANUAL

    }

    @Test
    fun testGetAggregation_Success() {

        ExponeaMockServer.setResponseSuccess("fetching/aggregation_success.json")

        var success = false
        var value: String? = null

        val customerIds = CustomerIds(cookie = "cookie")
        val attrs = CustomerAttributes(customerIds)
        attrs.withAggregation("aggregationId")

        runBlocking {
            ExponeaMockApi.fetchCustomerAttributes(attrs,
                    onSuccess = {
                        success = it.results.first().success
                        value = it.results.first().value
                    },
                    onFailure = {
                        success = false
                        value = it
                    })
        }

        val request = ExponeaMockServer.getResult()

        // TODO Assert real response
        assertEquals("/data/v2/projects/projectToken/customers/attributes", request.path)
        assertEquals(true, success)
        assertEquals("Aggregation", value)
    }

    @Test
    fun testGetAggregation_Failed() {

        ExponeaMockServer.setResponseError("fetching/expression_failure.json")

        var success = false
        var value: String? = null
        var error: String? = null
        val customerIds = CustomerIds(cookie = "cookie")
        val attrs = CustomerAttributes(customerIds)
        attrs.withAggregation("aggregationId")

        runBlocking {
            ExponeaMockApi.fetchCustomerAttributes(attrs,
                    onSuccess = {
                        success = it.results.first().success
                        value = it.results.first().value
                    },
                    onFailure = {
                        success = false
                        error = it
                    })
        }

        val request = ExponeaMockServer.getResult()

        // TODO Assert real response
        assertEquals("/data/v2/projects/projectToken/customers/attributes", request.path)
        assertEquals(false, success)
        assertNotNull(error)

    }
}