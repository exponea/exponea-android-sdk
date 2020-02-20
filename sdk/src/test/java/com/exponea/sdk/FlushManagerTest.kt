package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.ConnectionManagerMock
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.manager.ExponeaMockService
import com.exponea.sdk.manager.FlushManager
import com.exponea.sdk.manager.FlushManagerImpl
import com.exponea.sdk.manager.NoInternetConnectionManagerMock
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.DeviceProperties
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PropertiesList
import com.exponea.sdk.repository.EventRepository
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.currentTimeSeconds
import io.mockk.spyk
import io.mockk.verify
import kotlin.concurrent.thread
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FlushManagerTest : ExponeaSDKTest() {

    companion object {
        val configuration = ExponeaConfiguration()
        val server = ExponeaMockServer.createServer()

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

    private lateinit var properties: PropertiesList
    private lateinit var manager: FlushManager
    private lateinit var repo: EventRepository

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        properties = PropertiesList(properties = DeviceProperties(context).toHashMap())
        skipInstallEvent()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)

        repo = Exponea.component.eventRepository
        manager = Exponea.component.flushManager
    }

    @Test
    fun flushEvents_ShouldPass() {
        ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

        Exponea.trackEvent(
            eventType = Constants.EventTypes.sessionStart,
            timestamp = currentTimeSeconds(),
            properties = properties
        )
        waitForIt {
            manager.flushData { _ ->
                it.assertEquals(0, repo.all().size)
                it()
            }
        }
    }

    @Test
    fun flushEvents_ShouldFail_WithNoInternetConnection() {
        val service = ExponeaMockService(false)
        val noInternetManager = NoInternetConnectionManagerMock

        Exponea.trackEvent(
            eventType = Constants.EventTypes.sessionStart,
            timestamp = currentTimeSeconds(),
            properties = properties
        )

        // change the manager instance to one without internet access
        manager = FlushManagerImpl(configuration, repo, service, noInternetManager)

        waitForIt {
            manager.flushData { _ ->
                it.assertEquals(1, repo.all().size)
                it()
            }
        }
    }

    /**
     * When the servers fail to receive a event, it's deleted after 'configuration.maxTries' so
     * when the 'onFlushFinishListener' is called, it should be empty
     */
    @Test
    fun flushEvents_ShouldBeEmptyWhenItFails() {
        ExponeaMockServer.setResponseError(server, "tracking/track_event_failed.json")

        Exponea.trackEvent(
            eventType = Constants.EventTypes.sessionStart,
            timestamp = currentTimeSeconds(),
            properties = properties
        )

        waitForIt {
            manager.flushData { result ->
                it.assertEquals(0, repo.all().size)
                it()
            }
        }
    }

    @Test
    fun `should only flush once`() {
        val service = spyk(ExponeaMockService(true))
        manager = FlushManagerImpl(configuration, repo, service, ConnectionManagerMock)
        Exponea.trackEvent(
            eventType = "my event",
            timestamp = currentTimeSeconds(),
            properties = properties
        )

        waitForIt {
            var done = 0
            for (i in 1..10) {
                thread(start = true) {
                    manager.flushData {
                        if (++done == 10) it()
                    }
                }
            }
        }

        verify(exactly = 1) {
            service.postEvent(any(), any())
        }
    }
}
