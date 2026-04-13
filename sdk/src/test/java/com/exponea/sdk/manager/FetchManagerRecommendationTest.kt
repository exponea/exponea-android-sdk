package com.exponea.sdk.manager

import android.os.Build
import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.Result
import com.exponea.sdk.models.StreamConfig
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.ExponeaGson
import com.google.gson.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class FetchManagerRecommendationTest {

    val projectConfig = ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth")
    val streamConfig = StreamConfig("mock-base-url.com", "mock-stream-id")

    @Before
    fun setUp() {
    }

    fun getResponse(response: String): ResponseBody {
        return response.toResponseBody("application/json".toMediaTypeOrNull())
    }

    private fun runTest(
        integrationConfig: IntegrationConfig,
        mockSuccess: Boolean = true,
        mockResponse: String,
        expectedResult: Result<ArrayList<CustomerRecommendation>>? = null,
        expectedErrorResult: Result<FetchError>? = null
    ) {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(mockSuccess, getResponse(mockResponse)),
                ExponeaGson.instance
            ).fetchRecommendation(
                integrationConfig,
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
                { result ->
                    if (expectedResult == null) {
                        it.fail("Unexpected result")
                    }
                    it.assertEquals(expectedResult, result)
                    it()
                },
                { result: Result<FetchError> ->
                    if (expectedErrorResult == null) {
                        it.fail("Unexpected result")
                    }

                    it.assertEquals(expectedErrorResult, result)
                    it()
                }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should return error for non-existing user using project integration`() {
        val payload = """
        {
          "errors": {
            "_global": [
              "Customer does not exist"
            ]
          },
          "success": false
        }
        """
        runTest(
            integrationConfig = projectConfig,
            mockResponse = payload,
            expectedErrorResult = Result(
                false,
                FetchError(null, "Failure state from server returned")
            )
        )
    }

    @Test
    fun `should return error for non-existing recommendation using project integration`() {
        val payload = """
        {
          "results": [
            {
              "error": "Not Found",
              "success": false
            }
          ],
          "success": true
        }
        """
        runTest(
            integrationConfig = projectConfig,
            mockResponse = payload,
            expectedErrorResult = Result(false, FetchError(null, "Not Found"))
        )
    }

    @Test
    fun `should return same error message as in response payload using stream integration`() {
        val payload = """
            {
              "data": null,
              "errors": "Unexpected error occurred",
              "success": false
            }
        """
        runTest(
            integrationConfig = streamConfig,
            mockResponse = payload,
            expectedErrorResult = Result(
                false,
                FetchError(null, "Unexpected error occurred")
            )
        )
    }

    @Test
    fun `should return result for recommendation using project integration`() {
        val payload = """
        {
          "results": [
            {
              "success": true,
              "value": [
                {
                  "description": "an awesome book",
                  "engine_name": "random",
                  "image": "no image available",
                  "item_id": "1",
                  "name": "book",
                  "price": 19.99,
                  "product_id": "1",
                  "recommendation_id": "5dd6af3d147f518cb457c63c",
                  "recommendation_variant_id": null
                },
                {
                  "description": "super awesome off-brand phone",
                  "engine_name": "random",
                  "image": "just google one",
                  "item_id": "3",
                  "name": "mobile phone",
                  "price": 499.99,
                  "product_id": "3",
                  "recommendation_id": "5dd6af3d147f518cb457c63c",
                  "recommendation_variant_id": "mock id"
                }
              ]
            }
          ],
          "success": true
        }
        """
        runTest(
            integrationConfig = projectConfig,
            mockResponse = payload,
            expectedResult = Result(true, arrayListOf(
                CustomerRecommendation(
                    itemId = "1",
                    engineName = "random",
                    recommendationId = "5dd6af3d147f518cb457c63c",
                    recommendationVariantId = null,
                    data = hashMapOf(
                        "name" to JsonPrimitive("book"),
                        "description" to JsonPrimitive("an awesome book"),
                        "image" to JsonPrimitive("no image available"),
                        "price" to JsonPrimitive(19.99),
                        "product_id" to JsonPrimitive("1")
                    )
                ),
                CustomerRecommendation(
                    itemId = "3",
                    engineName = "random",
                    recommendationId = "5dd6af3d147f518cb457c63c",
                    recommendationVariantId = "mock id",
                    data = hashMapOf(
                        "name" to JsonPrimitive("mobile phone"),
                        "description" to JsonPrimitive("super awesome off-brand phone"),
                        "image" to JsonPrimitive("just google one"),
                        "price" to JsonPrimitive(499.99),
                        "product_id" to JsonPrimitive("3")
                    )
                )
            ))
        )
    }

    @Test
    fun `should return result for recommendation using stream integration`() {
        val payload = """
        {
          "data": [
            {
              "description": "an awesome book",
              "engine_name": "random",
              "image": "no image available",
              "item_id": "1",
              "name": "book",
              "price": 19.99,
              "product_id": "1",
              "recommendation_id": "5dd6af3d147f518cb457c63c",
              "recommendation_variant_id": null
            },
            {
              "description": "super awesome off-brand phone",
              "engine_name": "random",
              "image": "just google one",
              "item_id": "3",
              "name": "mobile phone",
              "price": 499.99,
              "product_id": "3",
              "recommendation_id": "5dd6af3d147f518cb457c63c",
              "recommendation_variant_id": "mock id"
            }
          ],
          "errors": "[]",
          "success": true
        }
        """
        runTest(
            integrationConfig = streamConfig,
            mockResponse = payload,
            expectedResult = Result(
                true, arrayListOf(
                    CustomerRecommendation(
                        itemId = "1",
                        engineName = "random",
                        recommendationId = "5dd6af3d147f518cb457c63c",
                        recommendationVariantId = null,
                        data = hashMapOf(
                            "name" to JsonPrimitive("book"),
                            "description" to JsonPrimitive("an awesome book"),
                            "image" to JsonPrimitive("no image available"),
                            "price" to JsonPrimitive(19.99),
                            "product_id" to JsonPrimitive("1")
                        )
                    ),
                    CustomerRecommendation(
                        itemId = "3",
                        engineName = "random",
                        recommendationId = "5dd6af3d147f518cb457c63c",
                        recommendationVariantId = "mock id",
                        data = hashMapOf(
                            "name" to JsonPrimitive("mobile phone"),
                            "description" to JsonPrimitive("super awesome off-brand phone"),
                            "image" to JsonPrimitive("just google one"),
                            "price" to JsonPrimitive(499.99),
                            "product_id" to JsonPrimitive("3")
                        )
                    )
                )
            )
        )
    }
}
