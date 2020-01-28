package com.exponea.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ExponeaTrackPushDeliveredTest(
    private val name: String,
    private val notificationData: NotificationData?,
    private val eventName: String,
    private val eventProperties: HashMap<String, Any>,
    private val eventType: EventType
) : ExponeaSDKTest() {
    companion object {
        data class TestCase(
            val name: String,
            val notificationData: NotificationData?,
            val eventName: String,
            val eventProperties: HashMap<String, Any>,
            val eventType: EventType
        )

        private val testCases = arrayListOf(
            TestCase(
                "empty push data",
                null,
                Constants.EventTypes.push,
                hashMapOf("status" to "delivered", "platform" to "android"),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "custom event type",
                NotificationData(eventType = "my_push_event"),
                "my_push_event",
                hashMapOf("status" to "delivered", "platform" to "android"),
                EventType.TRACK_EVENT
            ),
            TestCase(
                "custom platform",
                NotificationData(platform = "custom platform"),
                Constants.EventTypes.push,
                hashMapOf("status" to "delivered", "platform" to "custom platform"),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "empty event type",
                NotificationData(eventType = ""),
                Constants.EventTypes.push,
                hashMapOf("status" to "delivered", "platform" to "android"),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "full data",
                NotificationData(
                    campaignId = "mock campaign id",
                    campaignName = "mock campaign name",
                    actionId = 123456,
                    actionName = "mock action name",
                    actionType = "mock action type",
                    campaignPolicy = "mock campaign policy",
                    platform = "mock platform",
                    language = "mock language",
                    recipient = "mock recipient",
                    subject = "mock title"
                ),
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "delivered",
                    "campaign_id" to "mock campaign id",
                    "campaign_name" to "mock campaign name",
                    "action_id" to 123456L,
                    "action_name" to "mock action name",
                    "action_type" to "mock action type",
                    "campaign_policy" to "mock campaign policy",
                    "platform" to "mock platform",
                    "language" to "mock language",
                    "recipient" to "mock recipient",
                    "subject" to "mock title"
                ),
                EventType.PUSH_DELIVERED
            )
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Tracking {0}")
        fun data(): List<Array<out Any?>> {
            return testCases.map {
                arrayOf(
                    it.name,
                    it.notificationData,
                    it.eventName,
                    it.eventProperties,
                    it.eventType
                )
            }
        }
    }
    @Before
    fun before() {
        mockkConstructor(EventManagerImpl::class)
        skipInstallEvent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, FlushManagerTest.configuration)
    }

    @After
    fun after() {
        unmockkAll()
    }

    @Test
    fun `should track push delivered with empty data`() {
        val eventSlot = slot<ExportedEventType>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot))
        } just Runs
        Exponea.trackDeliveredPush(notificationData)
        assertEquals(eventName, eventSlot.captured.type)
        assertEquals(eventProperties, eventSlot.captured.properties)
        assertEquals(eventType, eventTypeSlot.captured)
    }
}
