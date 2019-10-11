package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.*
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
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
internal class FlushManagerTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()
        val properties = PropertiesList(properties = DeviceProperties().toHashMap())
        val server = MockWebServer()

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.baseURL = server.url("/").toString()
            configuration.projectToken = "projectToken"
            configuration.authorization = "projectAuthorization"
            configuration.maxTries = 1
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    private lateinit var manager: FlushManager
    private lateinit var repo: EventRepository

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        skipInstallEvent()
        Exponea.init(context, configuration)
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository
        manager = Exponea.component.flushManager
    }

    @Test
    fun flushEvents_ShouldPass() {
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")
        val count = CountDownLatch(1)

        Exponea.trackEvent(
            eventType = Constants.EventTypes.sessionStart,
            timestamp = currentTimeSeconds(),
            properties = properties
        )

        manager.onFlushFinishListener = {
            assertEquals(0, repo.all().size)
            count.countDown()
        }
        manager.flushData()

        count.await()
    }

    @Test(timeout = 30_000)
    fun flushEvents_ShouldFail_WithNoInternetConnection() {

        val count = CountDownLatch(1)
        val service = ExponeaMockService(false)
        val noInternetManager = NoInternetConnectionManagerMock

        Exponea.trackEvent(
            eventType = Constants.EventTypes.sessionStart,
            timestamp = currentTimeSeconds(),
            properties = properties
        )

        //change the manager instance to one without internet access
        manager = FlushManagerImpl(configuration, repo, service, noInternetManager)

        manager.onFlushFinishListener = {
            assertEquals(1, repo.all().size)
            count.countDown()
        }

        manager.flushData()
        count.await()
    }

    /**
     * When the servers fail to receive a event, it's deleted after 'configuration.maxTries' so
     * when the 'onFlushFinishListener' is called, it should be empty
     */
    @Test
    fun flushEvents_ShouldBeEmptyWhenItFails() {
        ExponeaMockServer.setResponseError(server, "tracking/track_event_failed.json")

        val count = CountDownLatch(1)

        Exponea.trackEvent(
            eventType = Constants.EventTypes.sessionStart,
            timestamp = currentTimeSeconds(),
            properties = properties
        )

        manager.onFlushFinishListener = {
            assertEquals(0, repo.all().size)
            count.countDown()
        }
        manager.flushData()

        count.await()
    }
}