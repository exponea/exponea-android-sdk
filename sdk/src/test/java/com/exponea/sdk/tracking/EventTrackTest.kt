package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.*
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.util.currentTimeSeconds
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class EventTrackTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()
        val properties = PropertiesList(properties = DeviceProperties().toHashMap())
        lateinit var server: MockWebServer

        @BeforeClass
        @JvmStatic
        fun setup() {
            server = MockWebServer()
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestTokenAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 3
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        skipInstallEvent()
        Exponea.init(context, configuration)
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository
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

        val lock = Object()
        Exponea.flushData()
        Exponea.component.flushManager.onFlushFinishListener = {
            assertEquals(0, repo.all().size)
            synchronized(lock) { lock.notify() }
        }
        synchronized(lock) { lock.wait() }

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
        val lock = Object()
        Exponea.component.flushManager.onFlushFinishListener = {
            assertEquals(1, repo.all().size)
            assertEquals(1, repo.all()[0].tries)
            synchronized(lock) { lock.notify() }
        }
        Exponea.flushData()

        synchronized(lock) { lock.wait() }
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

        val lock = Object()

        Exponea.component.flushManager.onFlushFinishListener = {
            assertEquals(0, repo.all().size)
            synchronized(lock) { lock.notify() }
        }
        Exponea.flushData()
        synchronized(lock) { lock.wait() }
        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
    }
}
