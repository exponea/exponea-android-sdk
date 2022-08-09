package com.exponea.sdk.manager

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.ExponeaExtras
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationAction.Companion.ACTION_TYPE_BUTTON
import com.exponea.sdk.models.NotificationAction.Companion.ACTION_TYPE_NOTIFICATION
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.PushNotificationRepository
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.data.NotificationTestPayloads
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class FcmManagerImplNotificationsTest(
    @Suppress("UNUSED_PARAMETER")
    name: String,
    private val notificationPayload: Map<String, String>,
    private val expectedTrackingData: NotificationData?,
    private val expectNotificationCreated: Boolean,
    private val expectedNotificationId: Int,
    private val expectedNotificationMatcher: (Notification) -> Unit,
    private val expectedNotificationData: Map<String, Any>?
) : ExponeaSDKTest() {
    private lateinit var manager: FcmManager
    private lateinit var pushNotificationRepository: PushNotificationRepository
    private lateinit var notificationManager: NotificationManager

    companion object {
        data class TestCase(
            val name: String,
            val notificationPayload: Map<String, String>,
            val expectNotificationCreated: Boolean,
            val expectedNotificationId: Int,
            val expectedNotificationMatcher: (Notification) -> Unit,
            val expectedTrackingData: NotificationData?,
            val expectedNotificationData: Map<String, Any>? = null
        )

        private fun validateIntent(
            intent: Intent,
            expectedAction: String,
            expectedNotificationAction: NotificationAction,
            expectedNotificationData: NotificationData?,
            expectedPayload: HashMap<String, String>
        ) {
            assertEquals(expectedAction, intent.action)
            assertEquals(expectedNotificationAction, intent.extras?.get(ExponeaExtras.EXTRA_ACTION_INFO))
            assertEquals(expectedNotificationData, intent.extras?.get(ExponeaExtras.EXTRA_DATA))
            assertEquals(expectedPayload, intent.extras?.get(ExponeaExtras.EXTRA_CUSTOM_DATA))
        }

        private val testCases = arrayListOf(
            TestCase(
                name = "basic notification",
                notificationPayload = NotificationTestPayloads.BASIC_NOTIFICATION,
                expectNotificationCreated = true,
                expectedNotificationId = 0,
                expectedNotificationMatcher = {
                    assertEquals("push title", shadowOf(it).contentTitle)
                    assertEquals("push notification message", shadowOf(it).contentText)
                    validateIntent(
                        shadowOf(it.contentIntent).savedIntent,
                        ExponeaExtras.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_NOTIFICATION),
                        NotificationData(),
                        NotificationTestPayloads.BASIC_NOTIFICATION
                    )

                    assertNull(it.actions)
                },
                expectedTrackingData = NotificationData()
            ),

            TestCase(
                name = "notification with deeplink",
                notificationPayload = NotificationTestPayloads.DEEPLINK_NOTIFICATION,
                expectNotificationCreated = true,
                expectedNotificationId = 0,
                expectedNotificationMatcher = {
                    assertEquals("push title", shadowOf(it).contentTitle)
                    assertEquals("push notification message", shadowOf(it).contentText)

                    validateIntent(
                        shadowOf(it.contentIntent).savedIntent,
                        ExponeaExtras.ACTION_DEEPLINK_CLICKED,
                        NotificationAction(ACTION_TYPE_NOTIFICATION, null, "app://test"),
                        NotificationData(),
                        NotificationTestPayloads.DEEPLINK_NOTIFICATION
                    )

                    assertNull(it.actions)
                },
                expectedTrackingData = NotificationData()
            ),

            TestCase(
                name = "notification with web url",
                notificationPayload = NotificationTestPayloads.BROWSER_NOTIFICATION,
                expectNotificationCreated = true,
                expectedNotificationId = 0,
                expectedNotificationMatcher = {
                    assertEquals("push title", shadowOf(it).contentTitle)
                    assertEquals("push notification message", shadowOf(it).contentText)

                    validateIntent(
                        shadowOf(it.contentIntent).savedIntent,
                        ExponeaExtras.ACTION_URL_CLICKED,
                        NotificationAction(ACTION_TYPE_NOTIFICATION, null, "http://google.com"),
                        NotificationData(),
                        NotificationTestPayloads.BROWSER_NOTIFICATION
                    )

                    assertNull(it.actions)
                },
                expectedTrackingData = NotificationData()
            ),

            TestCase(
                name = "notification with actions",
                notificationPayload = NotificationTestPayloads.ACTIONS_NOTIFICATION,
                expectNotificationCreated = true,
                expectedNotificationId = 0,
                expectedNotificationMatcher = {
                    assertEquals("push title", shadowOf(it).contentTitle)
                    assertEquals("push notification message", shadowOf(it).contentText)

                    validateIntent(
                        shadowOf(it.contentIntent).savedIntent,
                        ExponeaExtras.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_NOTIFICATION),
                        NotificationData(),
                        NotificationTestPayloads.ACTIONS_NOTIFICATION
                    )

                    assertEquals(3, it.actions.size)
                    assertEquals("Action 1 title", it.actions[0].title)
                    validateIntent(
                        shadowOf(it.actions[0].actionIntent).savedIntent,
                        ExponeaExtras.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 1 title"),
                        NotificationData(),
                        NotificationTestPayloads.ACTIONS_NOTIFICATION
                    )
                    assertEquals("Action 2 title", it.actions[1].title)
                    validateIntent(
                        shadowOf(it.actions[1].actionIntent).savedIntent,
                        ExponeaExtras.ACTION_DEEPLINK_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 2 title", "app://deeplink"),
                        NotificationData(),
                        NotificationTestPayloads.ACTIONS_NOTIFICATION
                    )
                    assertEquals("Action 3 title", it.actions[2].title)
                    validateIntent(
                        shadowOf(it.actions[2].actionIntent).savedIntent,
                        ExponeaExtras.ACTION_URL_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 3 title", "http://google.com"),
                        NotificationData(),
                        NotificationTestPayloads.ACTIONS_NOTIFICATION
                    )
                },
                expectedTrackingData = NotificationData()
            ),

            TestCase(
                "notification from production",
                NotificationTestPayloads.PRODUCTION_NOTIFICATION,
                true,
                1,
                {
                    val notificationData = NotificationData(
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
                            )
                    )
                    assertEquals("Notification title", shadowOf(it).contentTitle)
                    assertEquals("Notification text", shadowOf(it).contentText)
                    validateIntent(
                        shadowOf(it.contentIntent).savedIntent,
                        ExponeaExtras.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_NOTIFICATION),
                        notificationData,
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION
                    )

                    assertEquals(3, it.actions.size)
                    assertEquals("Action 1 title", it.actions[0].title)
                    validateIntent(
                        shadowOf(it.actions[0].actionIntent).savedIntent,
                        ExponeaExtras.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 1 title"),
                        notificationData,
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION
                    )
                    assertEquals("Action 2 title", it.actions[1].title)
                    validateIntent(
                        shadowOf(it.actions[1].actionIntent).savedIntent,
                        ExponeaExtras.ACTION_DEEPLINK_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 2 title", "http://deeplink?search=something"),
                        notificationData,
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION
                    )
                    assertEquals("Action 3 title", it.actions[2].title)
                    validateIntent(
                        shadowOf(it.actions[2].actionIntent).savedIntent,
                        ExponeaExtras.ACTION_URL_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 3 title", "http://google.com?search=something"),
                        notificationData,
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION
                    )
                },
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
                            )
                    ),
                mapOf(
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
                    "sent_timestamp" to 1.6145854222E9,
                    "type" to "push"
                )
            ),
            TestCase(
                name = "silent notification",
                notificationPayload = NotificationTestPayloads.SILENT_NOTIFICATION,
                expectNotificationCreated = false,
                expectedNotificationId = 0,
                expectedNotificationMatcher = {},
                expectedTrackingData = NotificationData(hashMapOf("silent_test" to "value")),
                expectedNotificationData = mapOf("silent_test" to "value")
            ),
                TestCase(
                        "production notification without sent_timestamp and type",
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION_WITHOUT_SENT_TIME_AND_TYPE,
                        true,
                        1,
                        {
                            val notificationData = NotificationData(
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
                                            "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z" // ktlint-disable max-line-lengt
                                    ),
                                    campaignMap = mapOf(
                                            "utm_source" to "exponea",
                                            "utm_campaign" to "Testing mobile push",
                                            "utm_medium" to "mobile_push_notification"
                                    )
                            )
                            assertEquals("Notification title", shadowOf(it).contentTitle)
                            assertEquals("Notification text", shadowOf(it).contentText)
                            validateIntent(
                                    shadowOf(it.contentIntent).savedIntent,
                                    ExponeaExtras.ACTION_CLICKED,
                                    NotificationAction(ACTION_TYPE_NOTIFICATION),
                                    notificationData,
                                    NotificationTestPayloads.PRODUCTION_NOTIFICATION_WITHOUT_SENT_TIME_AND_TYPE
                            )
                            assertEquals(3, it.actions.size)
                            assertEquals("Action 1 title", it.actions[0].title)
                            validateIntent(
                                    shadowOf(it.actions[0].actionIntent).savedIntent,
                                    ExponeaExtras.ACTION_CLICKED,
                                    NotificationAction(ACTION_TYPE_BUTTON, "Action 1 title"),
                                    notificationData,
                                    NotificationTestPayloads.PRODUCTION_NOTIFICATION_WITHOUT_SENT_TIME_AND_TYPE
                            )
                            assertEquals("Action 2 title", it.actions[1].title)
                            validateIntent(
                                    shadowOf(it.actions[1].actionIntent).savedIntent,
                                    ExponeaExtras.ACTION_DEEPLINK_CLICKED,
                                    NotificationAction(ACTION_TYPE_BUTTON, "Action 2 title", "http://deeplink?search=something"), // ktlint-disable max-line-lengt
                                    notificationData,
                                    NotificationTestPayloads.PRODUCTION_NOTIFICATION_WITHOUT_SENT_TIME_AND_TYPE
                            )
                            assertEquals("Action 3 title", it.actions[2].title)
                            validateIntent(
                                    shadowOf(it.actions[2].actionIntent).savedIntent,
                                    ExponeaExtras.ACTION_URL_CLICKED,
                                    NotificationAction(ACTION_TYPE_BUTTON, "Action 3 title", "http://google.com?search=something"), // ktlint-disable max-line-lengt
                                    notificationData,
                                    NotificationTestPayloads.PRODUCTION_NOTIFICATION_WITHOUT_SENT_TIME_AND_TYPE
                            )
                        },
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
                                        "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z" // ktlint-disable max-line-length
                                ),
                                campaignMap = mapOf(
                                        "utm_source" to "exponea",
                                        "utm_campaign" to "Testing mobile push",
                                        "utm_medium" to "mobile_push_notification"
                                )
                        ),
                        mapOf(
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
                                "platform" to "android"
                        )
                ),
                TestCase(
                        "notification with nested attributes",
                        NotificationTestPayloads.NOTIFICATION_WITH_NESTED_ATTRIBUTES,
                        true,
                        1,
                        {
                            val notificationData = NotificationData(
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
                                            "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-lengt
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
                                    )
                            )
                            assertEquals("Notification title", shadowOf(it).contentTitle)
                            assertEquals("Notification text", shadowOf(it).contentText)
                            validateIntent(
                                    shadowOf(it.contentIntent).savedIntent,
                                    ExponeaExtras.ACTION_CLICKED,
                                    NotificationAction(ACTION_TYPE_NOTIFICATION),
                                    notificationData,
                                    NotificationTestPayloads.NOTIFICATION_WITH_NESTED_ATTRIBUTES
                            )
                        },
                        NotificationData(
                                hashMapOf("event_type" to "campaign",
                                        "campaign_id" to "5db9ab54b073dfb424ccfa6f",
                                        "campaign_name" to "Wassil's push",
                                        "action_id" to 2.0,
                                        "action_name" to "Unnamed mobile push",
                                        "action_type" to "mobile notification",
                                        "campaign_policy" to "",
                                        "platform" to "android",
                                        "language" to "",
                                        "subject" to "Notification title",
                                        "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-lengt
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
                                )
                        ),
                        mapOf(
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
                        )
                ),
                TestCase(
                        "notification from production 002",
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION_2,
                        true,
                        1545339447,
                        {
                            val notificationData = NotificationData(
                                    dataMap = hashMapOf(
                                            "event_type" to "campaign",
                                            "campaign_id" to "5fc5439d3680dcf8ecf1fae1",
                                            "campaign_name" to "Use Case 001: alfa",
                                            "action_id" to 84.0,
                                            "action_name" to "Unnamed mobile push",
                                            "action_type" to "mobile notification",
                                            "campaign_policy" to "",
                                            "platform" to "android",
                                            "language" to "",
                                            "subject" to "Test sending",
                                            "recipient" to "dMALLSnQbHQ:APA91bEnnmqvcgy-89VPpVoik-Pt96jpg1HNLnVjSDfQvdQPiCYAUxH0xba6dTlDB0IGt1EcqcW8XgHMoywrmOUoLBZP_oL-mpJFvbQDgKsBPGdEPJHxIJ0HKXrbFkL-1GnjiqY6sA6q", // ktlint-disable max-line-length
                                            "sent_timestamp" to 1658403764.924152
                                    ),
                                    campaignMap = mapOf(
                                            "utm_source" to "exponea",
                                            "utm_campaign" to "Unnamed mobile push",
                                            "utm_medium" to "mobile_push_notification",
                                            "utm_content" to "hu"
                                    )
                            )
                        },
                        NotificationData(
                                dataMap = hashMapOf(
                                        "event_type" to "campaign",
                                        "campaign_id" to "5fc5439d3680dcf8ecf1fae1",
                                        "campaign_name" to "Use Case 001: alfa",
                                        "action_id" to 84.0,
                                        "action_name" to "Unnamed mobile push",
                                        "action_type" to "mobile notification",
                                        "campaign_policy" to "",
                                        "platform" to "android",
                                        "language" to "",
                                        "subject" to "Test sending",
                                        "recipient" to "dMALLSnQbHQ:APA91bEnnmqvcgy-89VPpVoik-Pt96jpg1HNLnVjSDfQvdQPiCYAUxH0xba6dTlDB0IGt1EcqcW8XgHMoywrmOUoLBZP_oL-mpJFvbQDgKsBPGdEPJHxIJ0HKXrbFkL-1GnjiqY6sA6q", // ktlint-disable max-line-length
                                        "sent_timestamp" to 1658403764.924152
                                ),
                                campaignMap = mapOf(
                                        "utm_source" to "exponea",
                                        "utm_campaign" to "Unnamed mobile push",
                                        "utm_medium" to "mobile_push_notification",
                                        "utm_content" to "hu"
                                )
                        ),
                        mapOf(
                                "campaign_name" to "Use Case 001: alfa",
                                "event_type" to "campaign",
                                "action_id" to 84.0,
                                "action_type" to "mobile notification",
                                "campaign_policy" to "",
                                "subject" to "Test sending",
                                "action_name" to "Unnamed mobile push",
                                "recipient" to "dMALLSnQbHQ:APA91bEnnmqvcgy-89VPpVoik-Pt96jpg1HNLnVjSDfQvdQPiCYAUxH0xba6dTlDB0IGt1EcqcW8XgHMoywrmOUoLBZP_oL-mpJFvbQDgKsBPGdEPJHxIJ0HKXrbFkL-1GnjiqY6sA6q", // ktlint-disable max-line-length
                                "language" to "",
                                "campaign_id" to "5fc5439d3680dcf8ecf1fae1",
                                "platform" to "android",
                                "sent_timestamp" to 1658403764.924152
                        )
                )
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Handling {0}")
        fun data(): List<Array<out Any?>> {
            return testCases.map {
                arrayOf(
                        it.name,
                        it.notificationPayload,
                        it.expectedTrackingData,
                        it.expectNotificationCreated,
                        it.expectedNotificationId,
                        it.expectedNotificationMatcher,
                        it.expectedNotificationData
                )
            }
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        pushNotificationRepository = PushNotificationRepositoryImpl(ExponeaPreferencesImpl(context))
        manager = FcmManagerImpl(
                context,
                ExponeaConfiguration(),
                mockkClass(EventManagerImpl::class),
                PushTokenRepositoryProvider.get(context),
                pushNotificationRepository
        )
        notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        mockkObject(Exponea)
        every { Exponea.trackDeliveredPush(any(), any()) } just Runs
        mockkConstructor(Date::class)
    }

    @Test
    fun handleRemoteMessageTest() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        val expectedCreationTime = expectedTrackingData!!.campaignData.createdAt * 1000
        every { anyConstructed<Date>().time } returns expectedCreationTime.toLong() // mock current time
        manager.handleRemoteMessage(notificationPayload, notificationManager, true)

        verify(exactly = 1) {
//            Exponea.trackDeliveredPush(any(), any())
            Exponea.trackDeliveredPush(expectedTrackingData, any())
        }
        verify(exactly = (if (expectNotificationCreated) 1 else 0)) {
            notificationManager.notify(expectedNotificationId, any())
        }
        if (expectNotificationCreated) {
            expectedNotificationMatcher(notificationSlot.captured)
        }
        assertEquals(expectedNotificationData, pushNotificationRepository.getExtraData())
    }
}
