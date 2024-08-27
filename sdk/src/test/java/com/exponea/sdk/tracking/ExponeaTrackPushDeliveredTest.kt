package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.manager.FcmManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.NotificationChannelImportance
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
    private val eventType: EventType,
    private val notifImportance: NotificationChannelImportance
) : ExponeaSDKTest() {
    companion object {
        data class TestCase(
            val name: String,
            val notificationData: NotificationData?,
            val eventName: String,
            val eventProperties: HashMap<String, Any>,
            val eventType: EventType,
            val notifImportance: NotificationChannelImportance = NotificationChannelImportance.DEFAULT
        )

        private val testCases = multiplyForImportance(arrayListOf(
            TestCase(
                "empty push data",
                null,
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "delivered",
                    "platform" to "android",
                    "state" to "shown",
                    "notification_importance" to "importance_default"
                ),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "custom event type",
                NotificationData(hashMapOf("event_type" to "my_push_event")),
                "my_push_event",
                hashMapOf(
                    "status" to "delivered",
                    "platform" to "android",
                    "state" to "shown",
                    "notification_importance" to "importance_default"
                ),
                EventType.TRACK_EVENT
            ),
            TestCase(
                "custom platform",
                NotificationData(hashMapOf("platform" to "custom platform")),
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "delivered",
                    "platform" to "custom platform",
                    "state" to "shown",
                    "notification_importance" to "importance_default"
                ),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "empty event type",
                NotificationData(hashMapOf("event_type" to "")),
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "delivered",
                    "platform" to "android",
                    "state" to "shown",
                    "notification_importance" to "importance_default"
                ),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "full data",
                NotificationData(
                    hashMapOf(
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
                    "state" to "shown",
                    "notification_importance" to "importance_default"
                ),
                EventType.PUSH_DELIVERED
            ),
            TestCase(
                "nested attributes",
                NotificationData(
                    hashMapOf(
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
                    "state" to "shown",
                    "notification_importance" to "importance_default",
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
        ))

        private fun multiplyForImportance(source: java.util.ArrayList<TestCase>): ArrayList<TestCase> {
            val target = arrayListOf<TestCase>()
            source.forEach { original ->
                // keep original
                target.add(original)
                // bring variations
                NotificationChannelImportance.values()
                    .filter {
                        // these are not testable as refers to UNSPECIFIED code and could be achieved on old Androids
                        it != NotificationChannelImportance.UNKNOWN && it != NotificationChannelImportance.UNSUPPORTED
                    }
                    .forEach {
                    if (it != original.notifImportance) {
                        val targetEventProps = original.eventProperties.toMutableMap()
                        if (targetEventProps.containsKey("notification_importance")) {
                            targetEventProps["notification_importance"] = it.trackValue
                        }
                        target.add(original.copy(
                            notifImportance = it,
                            name = "${original.name} ($it)",
                            eventProperties = HashMap(targetEventProps)
                        ))
                    }
                }
            }
            return target
        }

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Tracking {0}")
        fun data(): List<Array<out Any?>> {
            return testCases.map {
                arrayOf(
                    it.name,
                    it.notificationData,
                    it.eventName,
                    it.eventProperties,
                    it.eventType,
                    it.notifImportance
                )
            }
        }
    }
    @Before
    fun before() {
        mockkConstructorFix(EventManagerImpl::class) {
            every { anyConstructed<EventManagerImpl>().addEventToQueue(any(), any(), any()) }
        }
        skipInstallEvent()
    }

    @Test
    fun `should track push delivered with public API`() {
        initSdk(notifImportance)
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

    private fun initSdk(notifImportance: NotificationChannelImportance) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(
            projectToken = "mock-token",
            automaticSessionTracking = false,
            pushNotificationImportance = notifImportance.code
        )
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
        // ensure notif channel existance
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        (Exponea.componentForTesting.fcmManager as FcmManagerImpl).ensureNotificationChannelExistence(
            context,
            manager
        )
    }
}
