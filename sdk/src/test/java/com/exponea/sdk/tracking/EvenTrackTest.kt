package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.*
import com.exponea.sdk.repository.EventRepository
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.*
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class EvenTrackTest {

    lateinit var repo: EventRepository

    @Before
    fun init() {
        ExponeaMockServer.setUp()

        val context = RuntimeEnvironment.application

        val configuration = ExponeaConfiguration()
        configuration.baseURL = ExponeaMockServer.address
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        configuration.maxTries = 10

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository
        repo.clear()
    }

    @Test
    fun testEventAdded() {
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = Date().time,
                properties = DeviceProperties().toHashMap(),
                route = Route.TRACK_EVENTS)
        assertEquals(1, repo.all().size)

        val event = repo.all().first()
        assertEquals(Constants.EventTypes.sessionStart, event.item.type)

    }

    @Test
    fun testEventFlushed() {
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = Date().time,
                properties = DeviceProperties().toHashMap(),
                route = Route.TRACK_EVENTS)
        assertEquals(1, repo.all().size)
        ExponeaMockServer.setResponseSuccess("tracking/track_event_success.json")

        runBlocking {
            Exponea.flush()
            Exponea.component.flushManager.onFlushFinishListener = {
                assertEquals(0, repo.all().size)
            }
        }

    }

    @Test
    fun testEventFlushFailed() {
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = Date().time,
                properties = DeviceProperties().toHashMap(),
                route = Route.TRACK_EVENTS)
        assertEquals(1, repo.all().size)
        ExponeaMockServer.setResponseError("tracking/track_event_failed.json")
        runBlocking {
            Exponea.flush()
            Exponea.component.flushManager.onFlushFinishListener = {
                assertEquals(1, repo.all().size)
                val event = repo.all().first()
                assertEquals(Constants.EventTypes.sessionStart, event.item.type)
                assertEquals(1, event.tries)
            }
        }
    }

    @Test
    fun testEventMaxTriesReached() {
        Exponea.trackEvent(
                eventType = Constants.EventTypes.sessionStart,
                timestamp = Date().time,
                properties = DeviceProperties().toHashMap(),
                route = Route.TRACK_EVENTS)
        assertEquals(1, repo.all().size)
        val event = repo.all().first()
        event.tries = 10
        repo.update(event)
        ExponeaMockServer.setResponseError("tracking/track_event_failed.json")
        runBlocking {
            Exponea.flush()
            Exponea.component.flushManager.onFlushFinishListener = {
                assertEquals(0, repo.all().size)
            }
        }
    }
}