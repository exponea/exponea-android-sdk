package com.exponea.sdk.manager

import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.mocks.ExponeaMockService
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.ExponeaGson
import okhttp3.MediaType
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

    fun getResponse(response: String): ResponseBody {
        return ResponseBody.create(MediaType.get("application/json"), response)
    }

    @Test
    fun `should process fetch consents response`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(true, getResponse(consentsResponse)),
                ExponeaGson.instance
            ).fetchConsents(
                "mock-project-token",
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
    fun `should call onFailure when server returns invalid json`() {
        waitForIt {
            FetchManagerImpl(ExponeaMockService(true, getResponse("{{{{")), ExponeaGson.instance).fetchConsents(
                "mock-project-token",
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns empty json`() {
        waitForIt {
            FetchManagerImpl(ExponeaMockService(true, getResponse("{}")), ExponeaGson.instance).fetchConsents(
                "mock-project-token",
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }

    @Test
    fun `should call onFailure when server returns error code`() {
        waitForIt {
            FetchManagerImpl(
                ExponeaMockService(false, getResponse(consentsResponse)),
                ExponeaGson.instance
            ).fetchConsents(
                "mock-project-token",
                onSuccess = { _ -> it.fail("This should not happen") },
                onFailure = { _ -> it() }
            )
        }
    }
}
