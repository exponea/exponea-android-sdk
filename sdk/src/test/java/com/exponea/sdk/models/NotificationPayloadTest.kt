package com.exponea.sdk.models

import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.data.NotificationTestPayloads.ACTIONS_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.BASIC_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.BROWSER_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.DEEPLINK_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.NOTIFICATION_WITH_NESTED_ATTRIBUTES
import com.exponea.sdk.testutil.data.NotificationTestPayloads.PRODUCTION_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.SILENT_NOTIFICATION
import io.mockk.every
import java.util.Date
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class NotificationPayloadTest(
    @Suppress("UNUSED_PARAMETER")
    name: String,
    private val notificationPayload: HashMap<String, String>,
    private val validator: (NotificationPayload) -> Unit
) : ExponeaSDKTest() {
    companion object {
        data class TestCase(
            val name: String,
            val notificationPayload: HashMap<String, String>,
            val validator: (NotificationPayload) -> Unit
        )

        private val testCases = arrayListOf(
            TestCase(
                "empty notification",
                HashMap()
            ) {
                assertEquals(0, it.notificationId)
                assertEquals("", it.title)
                assertEquals("", it.message)
                assertEquals(null, it.image)
                assertEquals(null, it.sound)
                assertEquals(null, it.buttons)
                assertEquals(NotificationPayload.ActionPayload(), it.notificationAction)
                assertEquals(NotificationData(), it.notificationData)
                assertEquals(null, it.attributes)
            },
            TestCase(
                "basic notification",
                BASIC_NOTIFICATION
            ) {
                assertEquals(0, it.notificationId)
                assertEquals("push title", it.title)
                assertEquals("push notification message", it.message)
                assertEquals(null, it.image)
                assertEquals(null, it.sound)
                assertEquals(null, it.buttons)
                assertEquals(
                        NotificationPayload.ActionPayload(ExponeaNotificationActionType.APP),
                        it.notificationAction
                )
                assertEquals(NotificationData(), it.notificationData)
                assertEquals(null, it.attributes)
            },
            TestCase(
                "deeplink notification",
                DEEPLINK_NOTIFICATION
            ) {
                assertEquals(0, it.notificationId)
                assertEquals("push title", it.title)
                assertEquals("push notification message", it.message)
                assertEquals(null, it.image)
                assertEquals(null, it.sound)
                assertEquals(null, it.buttons)
                assertEquals(
                        NotificationPayload.ActionPayload(ExponeaNotificationActionType.DEEPLINK, "app://test"),
                        it.notificationAction
                )
                assertEquals(NotificationData(), it.notificationData)
                assertEquals(null, it.attributes)
            },
            TestCase(
                "browser notification",
                BROWSER_NOTIFICATION
            ) {
                assertEquals(0, it.notificationId)
                assertEquals("push title", it.title)
                assertEquals("push notification message", it.message)
                assertEquals(null, it.image)
                assertEquals(null, it.sound)
                assertEquals(null, it.buttons)
                assertEquals(
                        NotificationPayload.ActionPayload(ExponeaNotificationActionType.BROWSER, "http://google.com"),
                        it.notificationAction
                )
                assertEquals(NotificationData(), it.notificationData)
                assertEquals(null, it.attributes)
            },
            TestCase(
                "actions notification",
                ACTIONS_NOTIFICATION
            ) {
                assertEquals(0, it.notificationId)
                assertEquals("push title", it.title)
                assertEquals("push notification message", it.message)
                assertEquals(null, it.image)
                assertEquals(null, it.sound)
                assertEquals(
                        arrayListOf(
                                NotificationPayload.ActionPayload(
                                        ExponeaNotificationActionType.APP,
                                        null,
                                        "Action 1 title"
                                ),
                                NotificationPayload.ActionPayload(
                                        ExponeaNotificationActionType.DEEPLINK,
                                        "app://deeplink",
                                        "Action 2 title"
                                ),
                                NotificationPayload.ActionPayload(
                                        ExponeaNotificationActionType.BROWSER,
                                        "http://google.com",
                                        "Action 3 title"
                                )
                        ),
                        it.buttons
                )
                assertEquals(
                        NotificationPayload.ActionPayload(ExponeaNotificationActionType.APP),
                        it.notificationAction
                )
                assertEquals(NotificationData(), it.notificationData)
                assertEquals(null, it.attributes)
            },
            TestCase(
                "notification from production",
                PRODUCTION_NOTIFICATION
            ) {
                assertEquals(1, it.notificationId)
                assertEquals("Notification title", it.title)
                assertEquals("Notification text", it.message)
                assertEquals(null, it.image)
                assertEquals(null, it.sound)
                assertEquals(
                        arrayListOf(
                                NotificationPayload.ActionPayload(
                                        ExponeaNotificationActionType.APP,
                                        null,
                                        "Action 1 title"
                                ),
                                NotificationPayload.ActionPayload(
                                        ExponeaNotificationActionType.DEEPLINK,
                                        "http://deeplink?search=something",
                                        "Action 2 title"
                                ),
                                NotificationPayload.ActionPayload(
                                        ExponeaNotificationActionType.BROWSER,
                                        "http://google.com?search=something",
                                        "Action 3 title"
                                )
                        ),
                        it.buttons
                )
                assertEquals(
                        NotificationPayload.ActionPayload(ExponeaNotificationActionType.APP),
                        it.notificationAction
                )
                assertEquals(
                        NotificationData(
                                dataMap = hashMapOf("event_type" to "campaign",
                                        "campaign_id" to "5db9ab54b073dfb424ccfa6f",
                                        "campaign_name" to "Wassil's push",
                                        "action_id" to 2.0,
                                        "action_name" to "Unnamed mobile push",
                                        "action_type" to "mobile notification",
                                        "campaign_policy" to "",
                                        "platform" to "android",
                                        "language" to "",
                                        "subject" to "Notification title",
                                        "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-length
                                        "sent_timestamp" to 1614585422.20,
                                        "type" to "push"
                                ),
                                campaignMap = mapOf(
                                        "utm_source" to "exponea",
                                        "utm_campaign" to "Testing mobile push",
                                        "utm_medium" to "mobile_push_notification"
                                ),
                            consentCategoryTracking = null,
                            hasTrackingConsent = true
                        ),
                        it.notificationData
                )
                assertEquals(
                        hashMapOf(
                                "campaign_name" to "Wassil's push",
                                "event_type" to "campaign",
                                "action_id" to 2.0,
                                "action_type" to "mobile notification",
                                "campaign_policy" to "",
                                "subject" to "Notification title",
                                "action_name" to "Unnamed mobile push",
                                "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-length
                                "language" to "",
                                "campaign_id" to "5db9ab54b073dfb424ccfa6f",
                                "platform" to "android",
                                "sent_timestamp" to 1614585422.20,
                                "type" to "push"
                        ),
                        it.attributes
                )
            },
            TestCase(
                "silent notification",
                SILENT_NOTIFICATION
            ) {
                assertEquals(0, it.notificationId)
                assertEquals("Silent push", it.title)
                assertEquals("", it.message)
                assertEquals(null, it.image)
                assertEquals(null, it.sound)
                assertEquals(null, it.buttons)
                assertEquals(
                        NotificationPayload.ActionPayload(ExponeaNotificationActionType.APP),
                        it.notificationAction
                )
                assertEquals(NotificationData(hashMapOf("silent_test" to "value")), it.notificationData)
                assertEquals(mapOf("silent_test" to "value"), it.attributes)
            },
            TestCase(
                    "notification with nested attributes",
                    NOTIFICATION_WITH_NESTED_ATTRIBUTES
            ) {
                assertEquals(
                        NotificationData(
                                dataMap = hashMapOf("event_type" to "campaign",
                                        "campaign_id" to "5db9ab54b073dfb424ccfa6f",
                                        "campaign_name" to "Wassil's push",
                                        "action_id" to 2.0,
                                        "action_name" to "Unnamed mobile push",
                                        "action_type" to "mobile notification",
                                        "campaign_policy" to "",
                                        "platform" to "android",
                                        "language" to "",
                                        "subject" to "Notification title",
                                        "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-length
                                        "first_level_attribute" to hashMapOf(
                                                "second_level__nested_attribute" to hashMapOf(
                                                        "third_level_attribute" to "third_level_value"
                                                ),
                                                "second_level_attribute" to "second_level_value"
                                        ),
                                        "product_list" to arrayListOf(
                                                hashMapOf(
                                                        "item_id" to "1234",
                                                        "item_quantity" to 3.0
                                                ),
                                                hashMapOf(
                                                        "item_id" to "2345",
                                                        "item_quantity" to 2.0
                                                ),
                                                hashMapOf(
                                                        "item_id" to "6789",
                                                        "item_quantity" to 1.0
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
                                campaignMap = mapOf(
                                        "utm_source" to "exponea",
                                        "utm_campaign" to "Testing mobile push",
                                        "utm_medium" to "mobile_push_notification"
                                ),
                            consentCategoryTracking = null,
                            hasTrackingConsent = true
                        ),
                        it.notificationData
                )
                assertEquals(
                        hashMapOf(
                                "campaign_name" to "Wassil's push",
                                "event_type" to "campaign",
                                "action_id" to 2.0,
                                "action_type" to "mobile notification",
                                "campaign_policy" to "",
                                "subject" to "Notification title",
                                "action_name" to "Unnamed mobile push",
                                "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-length
                                "language" to "",
                                "campaign_id" to "5db9ab54b073dfb424ccfa6f",
                                "platform" to "android",
                                "first_level_attribute" to hashMapOf(
                                        "second_level__nested_attribute" to hashMapOf(
                                                "third_level_attribute" to "third_level_value"
                                        ),
                                        "second_level_attribute" to "second_level_value"
                                ),
                                "product_list" to arrayListOf(
                                        hashMapOf(
                                                "item_id" to "1234",
                                                "item_quantity" to 3.0
                                        ),
                                        hashMapOf(
                                                "item_id" to "2345",
                                                "item_quantity" to 2.0
                                        ),
                                        hashMapOf(
                                                "item_id" to "6789",
                                                "item_quantity" to 1.0
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
                        it.attributes
                )
            }

        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Parsing {0}")
        fun data(): List<Array<out Any?>> {
            return testCases.map {
                arrayOf(
                    it.name,
                    it.notificationPayload,
                    it.validator
                )
            }
        }
    }

    @Before
    fun setup() {
        mockkConstructorFix(Date::class)
        every { anyConstructed<Date>().time } returns 10 * 1000 // mock current time
    }

    @Test
    fun parseNotificationPayloadTest() {
        validator(NotificationPayload(notificationPayload))
    }
}
