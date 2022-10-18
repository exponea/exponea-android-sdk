package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.slot
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                NotificationData(hashMapOf("event_type" to "my_push_event")),
                "my_push_event",
                hashMapOf("status" to "delivered", "platform" to "android"),
                EventType.TRACK_EVENT
            ),
            TestCase(
                "custom platform",
                NotificationData(hashMapOf("platform" to "custom platform")),
                Constants.EventTypes.push,
                hashMapOf("status" to "delivered", "platform" to "custom platform"),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "empty event type",
                NotificationData(hashMapOf("event_type" to "")),
                Constants.EventTypes.push,
                hashMapOf("status" to "delivered", "platform" to "android"),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "full data",
                    NotificationData(hashMapOf(
                            "campaign_id" to "mock campaign id",
                            "campaign_name" to "mock campaign name",
                            "action_id" to 123456,
                            "action_name" to "mock action name",
                            "action_type" to "mock action type",
                            "campaign_policy" to "mock campaign policy",
                            "platform" to "mock platform",
                            "language" to "mock language",
                            "recipient" to "mock recipient",
                            "subject" to "mock title"
                    )),
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "delivered",
                    "campaign_id" to "mock campaign id",
                    "campaign_name" to "mock campaign name",
                    "action_id" to 123456,
                    "action_name" to "mock action name",
                    "action_type" to "mock action type",
                    "campaign_policy" to "mock campaign policy",
                    "platform" to "mock platform",
                    "language" to "mock language",
                    "recipient" to "mock recipient",
                    "subject" to "mock title"
                ),
                EventType.PUSH_DELIVERED
            ),
                TestCase(
                    "nested attributes",
                    NotificationData(hashMapOf(
                            "campaign_id" to "mock campaign id",
                            "campaign_name" to "mock campaign name",
                            "action_id" to 123456,
                            "action_name" to "mock action name",
                            "action_type" to "mock action type",
                            "campaign_policy" to "mock campaign policy",
                            "platform" to "mock platform",
                            "language" to "mock language",
                            "recipient" to "mock recipient",
                            "subject" to "mock title",
                            "product_list" to arrayListOf(
                                    hashMapOf(
                                            "item_id" to "1234",
                                            "item_quantity" to 3
                                    ),
                                    hashMapOf(
                                            "item_id" to "2345",
                                            "item_quantity" to 2
                                    ),
                                    hashMapOf(
                                            "item_id" to "6789",
                                            "item_quantity" to 1
                                    )
                            ),
                            "product_ids" to arrayListOf("1234", "2345", "6789"),
                            "push_content" to hashMapOf(
                                    "title" to "Hey!",
                                    "actions" to arrayListOf(
                                            hashMapOf(
                                                    "title" to "Action 1 title",
                                                    "action" to "app"
                                            )
                                    ),
                                    "message" to "We have a great deal for you today, don't miss it!"
                            )
                    )
                    ),
                Constants.EventTypes.push,
                hashMapOf(
                        "status" to "delivered",
                        "campaign_id" to "mock campaign id",
                        "campaign_name" to "mock campaign name",
                        "action_id" to 123456,
                        "action_name" to "mock action name",
                        "action_type" to "mock action type",
                        "campaign_policy" to "mock campaign policy",
                        "platform" to "mock platform",
                        "language" to "mock language",
                        "recipient" to "mock recipient",
                        "subject" to "mock title",
                        "product_list" to arrayListOf(
                                hashMapOf(
                                        "item_id" to "1234",
                                        "item_quantity" to 3
                                ),
                                hashMapOf(
                                        "item_id" to "2345",
                                        "item_quantity" to 2
                                ),
                                hashMapOf(
                                        "item_id" to "6789",
                                        "item_quantity" to 1
                                )
                        ),
                        "product_ids" to arrayListOf("1234", "2345", "6789"),
                        "push_content" to hashMapOf(
                                "title" to "Hey!",
                                "actions" to arrayListOf(
                                        hashMapOf(
                                                "title" to "Action 1 title",
                                                "action" to "app"
                                        )
                                ),
                                "message" to "We have a great deal for you today, don't miss it!"
                        )
                ),
                EventType.PUSH_DELIVERED
        ),
            TestCase(
                "Push without consent",
                NotificationData(hasTrackingConsent = false),
                Constants.EventTypes.push,
                hashMapOf(),
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
        val configuration = ExponeaConfiguration(projectToken = "mock-token", automaticSessionTracking = false)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
    }

    @Test
    fun `should track push delivered`() {
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot), true)
        } just Runs
        Exponea.trackDeliveredPush(notificationData)
        if (notificationData?.hasTrackingConsent != false) {
            assertEquals(eventName, eventSlot.captured.type)
            assertEquals(eventProperties, eventSlot.captured.properties)
            assertEquals(eventType, eventTypeSlot.captured)
        } else {
            assertFalse { eventSlot.isCaptured }
        }
    }
}
