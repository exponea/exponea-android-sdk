package com.exponea.sdk

import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.manager.FlushManager
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.EventRepository
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FlushManagerTest {

    companion object {
        val configuration = ExponeaConfiguration()
        val server = MockWebServer()

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.baseURL = server.url("/").toString()
            configuration.projectToken = "projectToken"
            configuration.authorization = "projectAuthorization"
        }

        @AfterClass
        fun tearDown() {
            server.shutdown()
        }
    }

    lateinit var manager: FlushManager
    lateinit var repo: EventRepository

    @Before
    fun init() {
        val context = RuntimeEnvironment.application

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository
        manager = Exponea.component.flushManager
    }

    @Test
    fun flushEvents_ShouldPass() {
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")
        val lock = CountDownLatch(1)
        manager.flushData()
        manager.onFlushFinishListener = {
            assertEquals(0, repo.all().size)
            lock.countDown()
        }
        lock.await()
    }

    @Test
    fun flushEvents_ShouldFail() {
        ExponeaMockServer.setResponseError(server, "tracking/track_event_failed.json")
        val lock = CountDownLatch(1)
        manager.flushData()
        manager.onFlushFinishListener = {
            assertEquals(1, repo.all().size)
            lock.countDown()
        }
        lock.await()
    }
}