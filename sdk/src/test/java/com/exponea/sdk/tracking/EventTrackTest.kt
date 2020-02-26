package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.currentTimeSeconds
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EventTrackTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()
        lateinit var server: MockWebServer

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = ExponeaMockServer.createServer()
            configuration.projectToken = "TestTokem"
            configuration.authorization = "Token TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 3
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    private lateinit var repo: EventRepository
    private lateinit var properties: PropertiesList

    @Before
    fun prepareForTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        properties = PropertiesList(properties = DeviceProperties(context).toHashMap())
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
        repo = Exponea.componentForTesting.eventRepository
    }

    @Test
    fun testEventAdded() {

        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = currentTimeSeconds(),
                properties = properties
        )

        assertEquals(1, repo.all().size)

        val event = repo.all().first()
        assertEquals(Constants.EventTypes.sessionStart, event.item.type)
    }

    @Test
    fun testEventFlushed() {
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                properties = properties
        )

        waitForIt {
            Exponea.flushData { _ ->
                assertEquals(0, repo.all().size)
                it()
            }
        }

        val request = server.takeRequest()

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
    }

    @Test
    fun testEventFlushFailed() {
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                properties = properties
        )
        assertEquals(1, repo.all().size)

        ExponeaMockServer.setResponseError(server, "tracking/track_event_failed.json")

        waitForIt {
            Exponea.flushData { _ ->
                assertEquals(1, repo.all().size)
                assertEquals(1, repo.all()[0].tries)
                it()
            }
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
    }

    @Test
    fun testEventMaxTriesReached() {
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = currentTimeSeconds(),
                properties = properties
        )

        assertEquals(1, repo.all().size)

        val event = repo.all().first()

        event.tries = 10
        repo.update(event)

        ExponeaMockServer.setResponseError(server, "tracking/track_event_failed.json")

        waitForIt {
            Exponea.flushData { _ ->
                assertEquals(0, repo.all().size)
                it()
            }
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
    }
}
