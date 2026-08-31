package com.exponea.sdk.manager

import android.os.Build
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.CustomerRecommendationOptions
import com.exponea.sdk.models.ProjectConfig
import com.exponea.sdk.models.SegmentTest
import com.exponea.sdk.models.SegmentationCategories
import com.exponea.sdk.network.ExponeaServiceImpl
import com.exponea.sdk.network.NetworkHandler
import com.exponea.sdk.network.auth.AuthStrategy
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.ExponeaGson
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
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
    override val stubAppInboxFetch = false

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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                syncToken = "mock-sync-token",
                applicationId = "default-application",
                onSuccess = { _ -> it() }
            ) { _ -> it.fail("This should not happen") }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                syncToken = "mock-sync-token",
                applicationId = "default-application",
                onSuccess = { _ -> it() }
            ) { _ -> it.fail("This should not happen") }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                syncToken = "mock-sync-token",
                applicationId = "default-application",
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                syncToken = "mock-sync-token",
                applicationId = "default-application",
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                syncToken = "mock-sync-token",
                applicationId = "default-application",
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                syncToken = "mock-sync-token",
                applicationId = "default-application",
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                syncToken = "mock-sync-token",
                applicationId = "default-application",
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { error ->
                it.assertEquals(400, error.results.httpCode)
                it()
            }
        }
    }

    @Test
    fun `should call onSuccess when server returns valid non-empty data for recommendations`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse(recommendationsResponse)),
                ExponeaGson.instance
            ).fetchRecommendation(
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                customerIds = hashMapOf("cookie" to "mock-cookie"),
                options = CustomerRecommendationOptions(id = "mock-id", fillWithRandom = true),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it() }
            ) { _ -> it.fail("This should not happen") }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                CustomerIds(hashMapOf("user" to "test")),
                "mock-sync-token",
                listOf("1"),
                onSuccess = { _ -> it.fail("This should not happen") }
            ) { _ -> it() }
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
            val emptyResponseInstance = ExponeaMockService(true, getResponse(SegmentTest.SEGMENTATIONS_JSON))
            val fetchManagerImpl = FetchManagerImpl(emptyResponseInstance, ExponeaGson.instance)
            fetchManagerImpl.fetchSegments(
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
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
                ProjectConfig("mock-base-url.com", "mock-project-token", "mock-auth"),
                invalidCustomerIds,
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should send etag and invoke onNotModified for in-app messages 304 revalidation`() {
        val etag = "\"inapp-etag\""
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", etag)
                .setBody("{\"success\":true,\"results\":[]}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(304)
                .setHeader("ETag", etag)
        )
        val fetchManagerImpl = FetchManagerImpl(
            ExponeaServiceImpl(ExponeaGson.instance, TestNetworkHandler()),
            ExponeaGson.instance
        )
        val projectConfig = ProjectConfig(server.url("/").toString(), "mock-project-token", "mock-auth")
        val customerIds = CustomerIds(hashMapOf("registered" to "test")).apply {
            cookie = "cookie-1"
        }
        var storedEtag: String? = null
        var notModifiedFired = false
        waitForIt { done ->
            fetchManagerImpl.fetchInAppMessages(
                integrationConfig = projectConfig,
                customerIds = customerIds,
                etag = null,
                onNotModified = null,
                onEtagHeader = { storedEtag = it },
                onSuccess = {
                    fetchManagerImpl.fetchInAppMessages(
                        integrationConfig = projectConfig,
                        customerIds = customerIds,
                        etag = storedEtag,
                        onNotModified = {
                            notModifiedFired = true
                            done()
                        },
                        onEtagHeader = { storedEtag = it },
                        onSuccess = { _ -> done.fail("Should have fired onNotModified") },
                        onFailure = { _ -> done.fail("This should not happen") }
                    )
                },
                onFailure = { _ -> done.fail("This should not happen") }
            )
        }
        assertEquals(2, server.requestCount)
        val firstRequest = server.takeRequest(1, TimeUnit.SECONDS)
        val secondRequest = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(firstRequest)
        assertNotNull(secondRequest)
        assertNull(firstRequest.getHeader("If-None-Match"))
        assertEquals(etag, secondRequest.getHeader("If-None-Match"))
        assertEquals(etag, storedEtag)
        assertTrue(notModifiedFired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should send etag and invoke onNotModified for personalized content 304 revalidation`() {
        val etag = "\"personalized-etag\""
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", etag)
                .setBody("{\"success\":true,\"results\":[]}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(304)
        )
        val fetchManagerImpl = FetchManagerImpl(
            ExponeaServiceImpl(ExponeaGson.instance, TestNetworkHandler()),
            ExponeaGson.instance
        )
        val projectConfig = ProjectConfig(server.url("/").toString(), "mock-project-token", "mock-auth")
        val customerIds = CustomerIds(hashMapOf("registered" to "test")).apply {
            cookie = "cookie-1"
        }
        val requestedBlockIds = listOf("block-a", "block-b")
        var storedEtag: String? = null
        var notModifiedFired = false
        waitForIt { done ->
            fetchManagerImpl.fetchPersonalizedContentBlocks(
                integrationConfig = projectConfig,
                customerIds = customerIds,
                contentBlockIds = requestedBlockIds,
                etag = null,
                onEtagHeader = { storedEtag = it },
                onSuccess = {
                    fetchManagerImpl.fetchPersonalizedContentBlocks(
                        integrationConfig = projectConfig,
                        customerIds = customerIds,
                        contentBlockIds = requestedBlockIds,
                        etag = storedEtag,
                        onNotModified = {
                            notModifiedFired = true
                            done()
                        },
                        onSuccess = { _ -> done.fail("Should have fired onNotModified") },
                        onFailure = { _ -> done.fail("This should not happen") }
                    )
                },
                onFailure = { _ -> done.fail("This should not happen") }
            )
        }
        assertEquals(2, server.requestCount)
        val firstRequest = server.takeRequest(1, TimeUnit.SECONDS)
        val secondRequest = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(firstRequest)
        assertNotNull(secondRequest)
        assertNull(firstRequest.getHeader("If-None-Match"))
        assertEquals(etag, secondRequest.getHeader("If-None-Match"))
        assertEquals(etag, storedEtag)
        assertTrue(notModifiedFired)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should not send If-None-Match when caller passes no etag`() {
        val etag = "\"inapp-etag\""
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", etag)
                .setBody("{\"success\":true,\"results\":[]}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", etag)
                .setBody("{\"success\":true,\"results\":[]}")
        )
        val fetchManagerImpl = FetchManagerImpl(
            ExponeaServiceImpl(ExponeaGson.instance, TestNetworkHandler()),
            ExponeaGson.instance
        )
        val projectConfig = ProjectConfig(server.url("/").toString(), "mock-project-token", "mock-auth")
        val customerIds = CustomerIds(hashMapOf("registered" to "test")).apply {
            cookie = "cookie-1"
        }
        waitForIt { done ->
            fetchManagerImpl.fetchInAppMessages(
                integrationConfig = projectConfig,
                customerIds = customerIds,
                etag = null,
                onNotModified = null,
                onEtagHeader = { },
                onSuccess = {
                    fetchManagerImpl.fetchInAppMessages(
                        integrationConfig = projectConfig,
                        customerIds = customerIds,
                        etag = null,
                        onNotModified = null,
                        onEtagHeader = { },
                        onSuccess = { done() },
                        onFailure = { _ -> done.fail("This should not happen") }
                    )
                },
                onFailure = { _ -> done.fail("This should not happen") }
            )
        }
        assertEquals(2, server.requestCount)
        val firstRequest = server.takeRequest(1, TimeUnit.SECONDS)
        val secondRequest = server.takeRequest(1, TimeUnit.SECONDS)
        assertNull(firstRequest?.getHeader("If-None-Match"))
        assertNull(secondRequest?.getHeader("If-None-Match"))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should report missing etag header on successful in-app messages fetch`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"results\":[]}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"success\":true,\"results\":[]}")
        )
        val fetchManagerImpl = FetchManagerImpl(
            ExponeaServiceImpl(ExponeaGson.instance, TestNetworkHandler()),
            ExponeaGson.instance
        )
        val projectConfig = ProjectConfig(server.url("/").toString(), "mock-project-token", "mock-auth")
        val customerIds = CustomerIds(hashMapOf("registered" to "test")).apply {
            cookie = "cookie-1"
        }
        var storedEtag: String? = "\"old-etag\""
        waitForIt { done ->
            fetchManagerImpl.fetchInAppMessages(
                integrationConfig = projectConfig,
                customerIds = customerIds,
                etag = null,
                onNotModified = null,
                onEtagHeader = { storedEtag = it },
                onSuccess = {
                    fetchManagerImpl.fetchInAppMessages(
                        integrationConfig = projectConfig,
                        customerIds = customerIds,
                        etag = storedEtag,
                        onNotModified = null,
                        onEtagHeader = { storedEtag = it },
                        onSuccess = { done() },
                        onFailure = { _ -> done.fail("This should not happen") }
                    )
                },
                onFailure = { _ -> done.fail("This should not happen") }
            )
        }
        assertEquals(2, server.requestCount)
        val firstRequest = server.takeRequest(1, TimeUnit.SECONDS)
        val secondRequest = server.takeRequest(1, TimeUnit.SECONDS)
        assertNull(firstRequest?.getHeader("If-None-Match"))
        assertNull(secondRequest?.getHeader("If-None-Match"))
        assertNull(storedEtag)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should update stored etag when server returns new ETag on 200`() {
        val firstEtag = "\"etag-v1\""
        val secondEtag = "\"etag-v2\""
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", firstEtag)
                .setBody("{\"success\":true,\"results\":[]}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("ETag", secondEtag)
                .setBody("{\"success\":true,\"results\":[]}")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(304)
                .setHeader("ETag", secondEtag)
        )
        val fetchManagerImpl = FetchManagerImpl(
            ExponeaServiceImpl(ExponeaGson.instance, TestNetworkHandler()),
            ExponeaGson.instance
        )
        val projectConfig = ProjectConfig(server.url("/").toString(), "mock-project-token", "mock-auth")
        val customerIds = CustomerIds(hashMapOf("registered" to "test")).apply {
            cookie = "cookie-1"
        }
        var storedEtag: String? = null
        var notModifiedFired = false
        waitForIt { done ->
            fetchManagerImpl.fetchInAppMessages(
                integrationConfig = projectConfig,
                customerIds = customerIds,
                etag = storedEtag,
                onNotModified = null,
                onEtagHeader = { storedEtag = it },
                onSuccess = {
                    fetchManagerImpl.fetchInAppMessages(
                        integrationConfig = projectConfig,
                        customerIds = customerIds,
                        etag = storedEtag,
                        onNotModified = null,
                        onEtagHeader = { storedEtag = it },
                        onSuccess = {
                            fetchManagerImpl.fetchInAppMessages(
                                integrationConfig = projectConfig,
                                customerIds = customerIds,
                                etag = storedEtag,
                                onNotModified = {
                                    notModifiedFired = true
                                    done()
                                },
                                onEtagHeader = { storedEtag = it },
                                onSuccess = { _ -> done.fail("Should have fired onNotModified") },
                                onFailure = { _ -> done.fail("This should not happen") }
                            )
                        },
                        onFailure = { _ -> done.fail("This should not happen") }
                    )
                },
                onFailure = { _ -> done.fail("This should not happen") }
            )
        }
        assertEquals(3, server.requestCount)
        val firstRequest = server.takeRequest(1, TimeUnit.SECONDS)
        val secondRequest = server.takeRequest(1, TimeUnit.SECONDS)
        val thirdRequest = server.takeRequest(1, TimeUnit.SECONDS)
        assertNull(firstRequest?.getHeader("If-None-Match"))
        assertEquals(firstEtag, secondRequest?.getHeader("If-None-Match"))
        assertEquals(secondEtag, thirdRequest?.getHeader("If-None-Match"))
        assertEquals(secondEtag, storedEtag)
        assertTrue(notModifiedFired)
    }

    private class TestNetworkHandler : NetworkHandler {
        private val client = OkHttpClient()
        private val mediaTypeJson = "application/json".toMediaType()

        override fun post(
            url: String,
            authStrategy: AuthStrategy,
            body: String,
            headers: Map<String, String>
        ): Call {
            return client.newCall(
                Request.Builder()
                    .url(url)
                    .apply {
                        headers.forEach { (name, value) -> addHeader(name, value) }
                    }
                    .post(body.toRequestBody(mediaTypeJson))
                    .build()
            )
        }

        override fun get(url: String, authStrategy: AuthStrategy): Call {
            return client.newCall(
                Request.Builder()
                    .url(url)
                    .get()
                    .build()
            )
        }
    }
}
