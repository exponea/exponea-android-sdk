package com.exponea.sdk.fetching

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class FetchAggregation {

    companion object {
        val configuration = ExponeaConfiguration()
        val attrs = CustomerAttributes()
        val server = MockWebServer()

        @BeforeClass @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestBasicAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")

            attrs.withAggregation("aggregationId")
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
    fun testGetAggregation_Success() {

        var success = false
        var value: String? = null

        ExponeaMockServer.setResponseSuccess(server, "fetching/aggregation_success.json")

        runBlocking {
            ExponeaMockApi.fetchCustomerAttributes(
                    attributes = attrs,
                    onSuccess = {
                        success = it.results.first().success
                        value = it.results.first().value
                    },
                    onFailure = {
                        success = it.success
                        value = it.results.jsonBody
                    })
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/data/v2/projects/TestTokem/customers/attributes", request.path)
        assertEquals(true, success)
        assertEquals("AggregationName", value)
    }

    @Test
    fun testGetAggregation_Failed() {

        ExponeaMockServer.setResponseError(server, "fetching/aggregation_failure.json")

        var success = false
        var value: String? = null
        var error: FetchError? = null

        runBlocking {
            ExponeaMockApi.fetchCustomerAttributes(
                    attributes = attrs,
                    onSuccess = {
                        success = it.results.first().success
                        value = it.results.first().value
                    },
                    onFailure = {
                        success = it.success
                        error = it.results
                    })
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/data/v2/projects/TestTokem/customers/attributes", request.path)
        assertEquals(false, success)
        assertNotNull(error)
        assertNull(value)
    }
}