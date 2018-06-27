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
class FetchSegment {

    companion object {
        val configuration = ExponeaConfiguration()
        val attrs = CustomerAttributes()
        val server = MockWebServer()

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestBasicAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")

            attrs.withSegmentation("SegmentationId")
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
    fun getSegment_ShouldSuccess() {

        // Set the response to success and json result.
        ExponeaMockServer.setResponseSuccess(server,"fetching/segmentation_success.json")

        var success: Boolean? = null
        var value: String? = null
        var error: FetchError? = null

        // Run blocking with coroutine to get the values from the async function.
        runBlocking {
            ExponeaMockApi.fetchCustomerId(
                    attributes = attrs,
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

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/data/v2/projects/TestTokem/customers/attributes", request.path)
        assertEquals(true, success)
        assertEquals("Segmentation", value)
        assertNull(error)
    }

    @Test
    fun getSegment_ShouldFailure() {

        // Set the response to success and json result.
        ExponeaMockServer.setResponseError(server,"fetching/segmentation_failure.json")

        var success: Boolean? = null
        var value: String? = null
        var error: FetchError? = null

        // Run blocking with coroutine to get the values from the async function.
        runBlocking {
            ExponeaMockApi.fetchCustomerId(
                    attributes = attrs,
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

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        // TODO assert real response
        assertEquals("/data/v2/projects/TestTokem/customers/attributes", request.path)
        assertNotNull(error)
        assertNull(value)
        assertEquals(false, success)
    }
}