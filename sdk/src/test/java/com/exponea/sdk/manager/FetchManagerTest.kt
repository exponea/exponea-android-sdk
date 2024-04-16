package com.exponea.sdk.manager

import android.os.Build
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.CustomerRecommendationRequest
import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.models.SegmentTest
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.ExponeaGson
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
internal class FetchManagerTest : ExponeaSDKTest() {
    private lateinit var server: MockWebServer

    @Before
    fun before() {
        server = ExponeaMockServer.createServer()
    }

    @After
    fun after() {
        server.shutdown()
    }

    val consentsResponse = """
    {
      "results": [
        {
          "id": "other",
          "legitimate_interest": false,
          "sources": {
            "crm": true,
            "import": true,
            "list_unsubscribe": true,
            "page": true,
            "private_api": true,
            "public_api": false,
            "scenario": true
          },
          "translations": {
            "": {
              "description": null,
              "name": "Other"
            }
          }
        }
      ],
      "success": true
    }
    """

    val recommendationsResponse = """
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

    fun getResponse(response: String?): ResponseBody? {
        return response?.toResponseBody("application/json".toMediaType())
    }

    @Test
    fun `should call onSuccess when server returns valid non-empty data for consents`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse(consentsResponse)),
                ExponeaGson.instance
            ).fetchConsents(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { result ->
                    it.assertEquals(1, result.results.size)
                    it.assertEquals(false, result.results[0].legitimateInterest)
                    it()
                },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns invalid json for consents`() {
        waitForIt {
            FetchManagerImpl(ExponeaMockService(true, getResponse("{{{{")), ExponeaGson.instance).fetchConsents(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns empty json for consents`() {
        waitForIt {
            FetchManagerImpl(ExponeaMockService(true, getResponse("{}")), ExponeaGson.instance).fetchConsents(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns false state for consents`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success: false, results:[]}")),
                ExponeaGson.instance
            ).fetchConsents(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns empty data json for consents`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success: true, results:[]}")),
                ExponeaGson.instance
            ).fetchConsents(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns raw-empty for consents`() {
        waitForIt {
            FetchManagerImpl(ExponeaMockService(true, getResponse("")), ExponeaGson.instance).fetchConsents(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns null for consents`() {
        waitForIt {
            FetchManagerImpl(ExponeaMockService(true, getResponse(null)), ExponeaGson.instance).fetchConsents(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns error code for consents`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(false, getResponse(consentsResponse)),
                ExponeaGson.instance
            ).fetchConsents(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns empty data json InApps`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success:true, results:[]}")),
                ExponeaGson.instance
            ).fetchInAppMessages(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns null data json InApps`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success:true}")),
                ExponeaGson.instance
            ).fetchInAppMessages(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns false state InApps`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse("{success:false}"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchInAppMessages(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns raw-empty InApps`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("")),
                ExponeaGson.instance
            ).fetchInAppMessages(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns invalid json InApps`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{{{")),
                ExponeaGson.instance
            ).fetchInAppMessages(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns null InApps`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse(null)),
                ExponeaGson.instance
            ).fetchInAppMessages(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns error InApps`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(false, getResponse("{}")),
                ExponeaGson.instance
            ).fetchInAppMessages(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns null data AppInbox`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{\"success\":true,\"sync_token\":\"CqIBDA...dxWvPKg\"}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchAppInbox(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns empty data AppInbox`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{\"success\":true,\"sync_token\":\"CqIBDA...dxWvPKg\", data:[]}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchAppInbox(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns false state AppInbox`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse("{success:false}"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchAppInbox(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns invalid json AppInbox`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse("{{{"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchAppInbox(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns raw-empty json AppInbox`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(""))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchAppInbox(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns null AppInbox`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(null))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchAppInbox(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns error AppInbox`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(false, getResponse("{}"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchAppInbox(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onSuccess when server returns valid non-empty data for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse(recommendationsResponse)),
                ExponeaGson.instance
            ).fetchRecommendation(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
                onSuccess = { result ->
                    it.assertEquals(2, result.results.size)
                    it.assertEquals("5dd6af3d147f518cb457c63c", result.results[0].recommendationId)
                    it()
                },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns invalid json for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{{{{")),
                ExponeaGson.instance
            ).fetchRecommendation(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns empty json for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{}")),
                ExponeaGson.instance
            ).fetchRecommendation(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns false state for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success: false, results:[]}")),
                ExponeaGson.instance
            ).fetchRecommendation(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns empty data json for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success: true, results:[]}")),
                ExponeaGson.instance
            ).fetchRecommendation(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns raw-empty for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("")),
                ExponeaGson.instance
            ).fetchRecommendation(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns null for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse(null)),
                ExponeaGson.instance
            ).fetchRecommendation(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns error code for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(false, getResponse(recommendationsResponse)),
                ExponeaGson.instance
            ).fetchRecommendation(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerRecommendationRequest(
                    customerIds = hashMapOf("cookie" to "mock-cookie"),
                    options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true)
                ),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns success state MarkAsRead AppInbox action`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success: true}")),
                ExponeaGson.instance
            ).markAppInboxAsRead(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns failure state MarkAsRead AppInbox action`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success: false}")),
                ExponeaGson.instance
            ).markAppInboxAsRead(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns invalid json MarkAsRead AppInbox action`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{{{")),
                ExponeaGson.instance
            ).markAppInboxAsRead(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns empty json MarkAsRead AppInbox action`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{}")),
                ExponeaGson.instance
            ).markAppInboxAsRead(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns raw-empty response MarkAsRead AppInbox action`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("")),
                ExponeaGson.instance
            ).markAppInboxAsRead(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns null response MarkAsRead AppInbox action`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse(null)),
                ExponeaGson.instance
            ).markAppInboxAsRead(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns null data Static-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{\"success\":true}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchStaticInAppContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns empty data Static-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{\"success\":true, data:[]}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchStaticInAppContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns false state Static-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse("{success:false}"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchStaticInAppContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns invalid json Static-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse("{{{"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchStaticInAppContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns raw-empty json Static-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(""))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchStaticInAppContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns null Static-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(null))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchStaticInAppContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns error Static-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(false, getResponse("{}"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchStaticInAppContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns null data Personalized-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{\"success\":true}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = CustomerIds(hashMapOf("user" to "test")),
                contentBlockIds = listOf("1"),
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns empty data Personalized-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{\"success\":true, data:[]}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = CustomerIds(hashMapOf("user" to "test")),
                contentBlockIds = listOf("1"),
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns false state Personalized-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse("{success:false}"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = CustomerIds(hashMapOf("user" to "test")),
                contentBlockIds = listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns invalid json Personalized-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse("{{{"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = CustomerIds(hashMapOf("user" to "test")),
                contentBlockIds = listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns raw-empty json Personalized-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(""))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = CustomerIds(hashMapOf("user" to "test")),
                contentBlockIds = listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns null Personalized-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(null))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = CustomerIds(hashMapOf("user" to "test")),
                contentBlockIds = listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns error Personalized-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(false, getResponse("{}"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = CustomerIds(hashMapOf("user" to "test")),
                contentBlockIds = listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess with empty data for empty message IDs request Personalized-InAppContentBlock`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{\"success\":true, data:[]}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = CustomerIds(hashMapOf("user" to "test")),
                contentBlockIds = emptyList(),
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns null data for Segments`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")).apply {
                    cookie = "mock-cookie"
                },
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns empty data for Segments`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(
                "{\"discovery\":[]}"
            ))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")).apply {
                    cookie = "mock-cookie"
                },
                onSuccess = { _ -> it() },
                onFailure = { _ -> it.fail("This should not happen") }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onSuccess when server returns some data for Segments`() {
        var parsedSegmentations = SegmentationCategories()
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(SegmentTest.segmentationsJson))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")).apply {
                    cookie = "mock-cookie"
                },
                onSuccess = { response ->
                    parsedSegmentations = response.results
                    it()
                },
                onFailure = { response -> it.fail("This should not happen: ${response.results.message}") }
            )
        }
        assertTrue(parsedSegmentations.isNotEmpty())
        assertEquals(3, parsedSegmentations.size)
        val discoverySegments = parsedSegmentations["discovery"]
        assertNotNull(discoverySegments)
        assertEquals(2, discoverySegments.size)
        assertEquals("val1", discoverySegments[0]["prop1"])
        assertEquals("2", discoverySegments[0]["prop2"])
        assertEquals("true", discoverySegments[0]["prop3"])
        assertEquals("valA", discoverySegments[1]["prop1"])
        assertEquals("two", discoverySegments[1]["prop2"])
        assertEquals("false", discoverySegments[1]["prop3"])
        val contentSegments = parsedSegmentations["content"]
        assertNotNull(contentSegments)
        assertEquals(2, contentSegments.size)
        assertEquals("val1", contentSegments[0]["cont1"])
        assertEquals("2", contentSegments[0]["cont2"])
        assertEquals("true", contentSegments[0]["cont3"])
        assertEquals("valA", contentSegments[1]["cont1"])
        assertEquals("two", contentSegments[1]["cont2"])
        assertEquals("false", contentSegments[1]["cont3"])
        val merchandisingSegments = parsedSegmentations["merchandising"]
        assertNotNull(merchandisingSegments)
        assertEquals(2, merchandisingSegments.size)
        assertEquals("val1", merchandisingSegments[0]["merch1"])
        assertEquals("2", merchandisingSegments[0]["merch2"])
        assertEquals("true", merchandisingSegments[0]["merch3"])
        assertEquals("valA", merchandisingSegments[1]["merch1"])
        assertEquals("two", merchandisingSegments[1]["merch2"])
        assertEquals("false", merchandisingSegments[1]["merch3"])
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns false state for Segments`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(false, getResponse(null))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")).apply {
                    cookie = "mock-cookie"
                },
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns invalid json for Segments`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse("{{{"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")).apply {
                    cookie = "mock-cookie"
                },
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns raw-empty json for Segments`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(""))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")).apply {
                    cookie = "mock-cookie"
                },
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns null for Segments`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(true, getResponse(null))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")).apply {
                    cookie = "mock-cookie"
                },
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure when server returns error for Segments`() {
        waitForIt {
            val emptyResponseInstance = ExponeaMockService(false, getResponse("{}"))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")).apply {
                    cookie = "mock-cookie"
                },
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call onFailure for customerIds without cookie`() {
        val invalidCustomerIds = CustomerIds().apply {
            externalIds = hashMapOf("registered" to "test")
        }
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse("{success: true, results:[]}")),
                ExponeaGson.instance
            ).fetchSegments(
                ExponeaProject("mock-base-url.com", "mock-project-token", "mock-auth"),
                invalidCustomerIds,
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }
}
