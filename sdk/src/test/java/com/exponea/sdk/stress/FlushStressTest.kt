package com.exponea.sdk.stress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.*
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
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
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FlushStressTest : ExponeaSDKTest() {
    companion object {
        val configuration = ExponeaConfiguration()
        val server = MockWebServer()
        const val stressCount = 500

        @BeforeClass
        @JvmStatic
        fun setup() {
            configuration.baseURL = server.url("/").toString()
            configuration.projectToken = "projectToken"
            configuration.authorization = "projectAuthorization"
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            server.shutdown()
        }
    }

    lateinit var manager: FlushManager
    lateinit var repo: EventRepository
    private lateinit var service: ExponeaMockService

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        skipInstallEvent()
        Exponea.init(context, configuration)
        waitUntilFlushed()
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository
        service = ExponeaMockService(true)
        manager = FlushManagerImpl(configuration, repo, service, ConnectionManagerMock)
        repo.clear()
    }

    @Test
    fun testFlushingStressed() {
        val r = Random()
        var insertedCount = 0
        for (i in 0 until stressCount) {

            ExponeaMockServer.setResponseSuccess(server, "tracking/track_event_success.json")

            val eventType = when {
                i % 7 == 0 -> Constants.EventTypes.sessionEnd
                i % 5 == 0 -> Constants.EventTypes.installation
                i % 3 == 0 -> Constants.EventTypes.sessionStart
                i % 2 == 0 -> Constants.EventTypes.payment
                else -> Constants.EventTypes.push
            }
            Exponea.trackEvent(
                eventType = eventType,
                timestamp = currentTimeSeconds(),
                properties = StressTest.properties
            )
            insertedCount++
            if (r.nextInt(10) == 3) {
                assertEquals(repo.all().size, insertedCount)
                insertedCount = 0
                val lock = CountDownLatch(1)
                manager.flushData()
                manager.onFlushFinishListener = {
                    assertEquals(0, repo.all().size)
                    lock.countDown()
                }
                lock.await()
            }
        }
    }
}