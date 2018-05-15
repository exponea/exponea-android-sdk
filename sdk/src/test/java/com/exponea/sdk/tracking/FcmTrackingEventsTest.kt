package com.exponea.sdk.tracking

import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.ExponeaMockServer
import com.exponea.sdk.models.CustomerIds
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.repository.EventRepository
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FcmTrackingEventsTest {

    private lateinit var repo: EventRepository

    @Before
    fun init() {
        ExponeaMockServer.setUp()

        val context = RuntimeEnvironment.application
        val configuration = ExponeaConfiguration()
        configuration.baseURL = ExponeaMockServer.address
        configuration.projectToken = "projectToken"
        configuration.authorization = "projectAuthorization"

        Exponea.init(context, configuration)

        Exponea.flushMode = FlushMode.MANUAL

        // Clear events repo for testing purposes
        repo = Exponea.component.eventRepository
        repo.clear()

    }

    @Test
    fun testTokenEventAdded() {
        // Track token
        Exponea.trackFcmToken(
                customerIds = CustomerIds(cookie = "cookie"),
                fcmToken = "ordinary token"
        )
        assertEquals(1, repo.all().size)

        val props = repo.all().first().item.properties
        val token =((props?.get("properties")) as HashMap<*, *>)["push_notification_token"]
        assertEquals("ordinary token", token)
    }

    @Test
    fun testTrackDeliveredPush() {

        // Track delivered push
        Exponea.trackDeliveredPush(
                customerIds = CustomerIds(cookie = "cookie"),
                fcmToken = "ordinary token")


        // Check if event was added to db
        val event = repo.all().first()
        assertEquals("push_delivered", event.item.type)
        assertEquals(1, repo.all().size)

        ExponeaMockServer.setResponseSuccess("tracking/track_event_success.json")

        // Flush this event and check it was sent successfully
        runBlocking {
            Exponea.component.flushManager.onFlushFinishListener = {
                assertEquals(0, repo.all().size)
                val result = ExponeaMockServer.getResult()
                assertEquals("/track/v2/projects/projectToken/customers/events", result.path)
            }
            Exponea.flush()
        }

    }

    @Test
    fun testTrackClickedPush() {
        // Track clicked push
        Exponea.trackClickedPush(
                customerIds = CustomerIds(cookie = "cookie"),
                fcmToken = "ordinary token"

        )

        // Check if event was added to db

        val event = repo.all().first()
        assertEquals("push_clicked", event.item.type)
        assertEquals(1, repo.all().size)

        ExponeaMockServer.setResponseSuccess("tracking/track_event_success.json")

        runBlocking {
            Exponea.component.flushManager.onFlushFinishListener = {
                assertEquals(0, repo.all().size)
                val result = ExponeaMockServer.getResult()
                assertEquals("/track/v2/projects/projectToken/customers/events", result.path)
            }
        }

    }

}
