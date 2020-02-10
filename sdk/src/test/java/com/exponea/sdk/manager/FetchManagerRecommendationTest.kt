package com.exponea.sdk.manager

import com.exponea.sdk.models.CustomerRecommendation
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.FetchError
import com.exponea.sdk.models.Result
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.ExponeaGson
import com.google.gson.JsonPrimitive
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FetchManagerRecommendationTest {

    @Before
    fun setUp() {
    }

    fun getResponse(response: String): ResponseBody {
        return ResponseBody.create(MediaType.get("application/json"), response)
    }

    private fun runTest(
        mockResponse: String,
        expectedResult: Result<ArrayList<CustomerRecommendation>>? = null,
        expectedErrorResult: Result<FetchError>? = null
    ) {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse(mockResponse)),
                ExponeaGson.instance
            ).fetchRecommendation(
                "mock-project-token",
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
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
    fun `should return error for non-existing user`() {
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
            mockResponse = payload,
            expectedErrorResult = Result(false, FetchError(payload, "Unable to parse response from the server."))
        )
    }

    @Test
    fun `should return error for non-existing recommendation`() {
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
            mockResponse = payload,
            expectedErrorResult = Result(false, FetchError(null, "Not Found"))
        )
    }

    @Test
    fun `should return result for recommendation`() {
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
}
