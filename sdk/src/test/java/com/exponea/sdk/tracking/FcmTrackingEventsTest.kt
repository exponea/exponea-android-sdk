package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockApi
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.EventRepository
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FcmTrackingEventsTest {

    companion object {
        val configuration = ExponeaConfiguration()
        const val token = "FirebaseCloudToken#"
        val server = MockWebServer()

        @BeforeClass @JvmStatic
        fun setup() {
            configuration.projectToken = "TestTokem"
            configuration.authorization = "TestBasicAuthentication"
            configuration.baseURL = server.url("").toString().substringBeforeLast("/")
            configuration.maxTries = 10
        }

        @AfterClass
        fun tearDown() {
            server.shutdown()
        }
    }

    private lateinit var repo: EventRepository

    @Before
    fun prepareForTest() {

        val context = RuntimeEnvironment.application

        Exponea.init(context, configuration)
        Exponea.flushMode = FlushMode.MANUAL

        repo = Exponea.component.eventRepository

        // Clean event repository for testing purposes
        repo.clear()
    }

    @Test
    fun testTokenEventAdded_ShouldSuccess() {
        // Track token
        Exponea.trackPushToken(
                fcmToken = token
        )

        assertEquals(1, repo.all().size)

        val props = repo.all().first().item.properties
        val token = props?.let {
            it["google_push_notification_id"]
        }
        assertEquals("FirebaseCloudToken#", token)
    }

    @Test
    fun testTrackDeliveredPush_ShouldSuccess() {
        // Track delivered push
        Exponea.trackDeliveredPush(
                fcmToken = token
        )

        // Check if event was added to db
        val event = repo.all().first()

        assertEquals("campaign", event.item.type)
        assertEquals(1, repo.all().size)

        ExponeaMockServer.setResponseSuccess(server,"tracking/track_event_success.json")

        // Flush this event and check it was sent successfully
        runBlocking {
            ExponeaMockApi.flush()
            Exponea.component.flushManager.onFlushFinishListener = {
                assertEquals(0, repo.all().size)
            }
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
    }

    @Test
    fun testTrackClickedPush_ShouldSuccess() {
        // Track clicked push
        Exponea.trackClickedPush(
                fcmToken = token
        )

        // Check if event was added to db
        val event = repo.all().first()

        assertEquals("campaign", event.item.type)
        assertEquals(1, repo.all().size)

        ExponeaMockServer.setResponseSuccess(server,"tracking/track_event_success.json")

        runBlocking {
            ExponeaMockApi.flush()
            Exponea.component.flushManager.onFlushFinishListener = {
                assertEquals(0, repo.all().size)
            }
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)

        assertEquals("/track/v2/projects/TestTokem/customers/events", request.path)
    }

}
