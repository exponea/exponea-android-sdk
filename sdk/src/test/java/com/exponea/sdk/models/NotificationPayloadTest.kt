package com.exponea.sdk.models

import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.data.NotificationTestPayloads.ACTIONS_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.BASIC_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.BROWSER_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.DEEPLINK_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.PRODUCTION_NOTIFICATION
import com.exponea.sdk.testutil.data.NotificationTestPayloads.SILENT_NOTIFICATION
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import java.util.Date
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class NotificationPayloadTest(
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
                HashMap(),
                {
                    assertEquals(0, it.notificationId)
                    assertEquals("", it.title)
                    assertEquals("", it.message)
                    assertEquals(null, it.image)
                    assertEquals(null, it.sound)
                    assertEquals(null, it.buttons)
                    assertEquals(NotificationPayload.ActionPayload(), it.notificationAction)
                    assertEquals(NotificationData(), it.notificationData)
                    assertEquals(null, it.attributes)
                }
            ),
            TestCase(
                "basic notification",
                BASIC_NOTIFICATION,
                {
                    assertEquals(0, it.notificationId)
                    assertEquals("push title", it.title)
                    assertEquals("push notification message", it.message)
                    assertEquals(null, it.image)
                    assertEquals(null, it.sound)
                    assertEquals(null, it.buttons)
                    assertEquals(
                        NotificationPayload.ActionPayload(NotificationPayload.Actions.APP),
                        it.notificationAction
                    )
                    assertEquals(NotificationData(), it.notificationData)
                    assertEquals(null, it.attributes)
                }
            ),
            TestCase(
                "deeplink notification",
                DEEPLINK_NOTIFICATION,
                {
                    assertEquals(0, it.notificationId)
                    assertEquals("push title", it.title)
                    assertEquals("push notification message", it.message)
                    assertEquals(null, it.image)
                    assertEquals(null, it.sound)
                    assertEquals(null, it.buttons)
                    assertEquals(
                        NotificationPayload.ActionPayload(NotificationPayload.Actions.DEEPLINK, "app://test"),
                        it.notificationAction
                    )
                    assertEquals(NotificationData(), it.notificationData)
                    assertEquals(null, it.attributes)
                }
            ),
            TestCase(
                "browser notification",
                BROWSER_NOTIFICATION,
                {
                    assertEquals(0, it.notificationId)
                    assertEquals("push title", it.title)
                    assertEquals("push notification message", it.message)
                    assertEquals(null, it.image)
                    assertEquals(null, it.sound)
                    assertEquals(null, it.buttons)
                    assertEquals(
                        NotificationPayload.ActionPayload(NotificationPayload.Actions.BROWSER, "http://google.com"),
                        it.notificationAction
                    )
                    assertEquals(NotificationData(), it.notificationData)
                    assertEquals(null, it.attributes)
                }
            ),
            TestCase(
                "actions notification",
                ACTIONS_NOTIFICATION,
                {
                    assertEquals(0, it.notificationId)
                    assertEquals("push title", it.title)
                    assertEquals("push notification message", it.message)
                    assertEquals(null, it.image)
                    assertEquals(null, it.sound)
                    assertEquals(
                        arrayListOf(
                            NotificationPayload.ActionPayload(
                                NotificationPayload.Actions.APP,
                                null,
                                "Action 1 title"
                            ),
                            NotificationPayload.ActionPayload(
                                NotificationPayload.Actions.DEEPLINK,
                                "app://deeplink",
                                "Action 2 title"
                            ),
                            NotificationPayload.ActionPayload(
                                NotificationPayload.Actions.BROWSER,
                                "http://google.com",
                                "Action 3 title"
                            )
                        ),
                        it.buttons
                    )
                    assertEquals(
                        NotificationPayload.ActionPayload(NotificationPayload.Actions.APP),
                        it.notificationAction
                    )
                    assertEquals(NotificationData(), it.notificationData)
                    assertEquals(null, it.attributes)
                }
            ),
            TestCase(
                "notification from production",
                PRODUCTION_NOTIFICATION,
                {
                    assertEquals(1, it.notificationId)
                    assertEquals("Notification title", it.title)
                    assertEquals("Notification text", it.message)
                    assertEquals(null, it.image)
                    assertEquals(null, it.sound)
                    assertEquals(
                        arrayListOf(
                            NotificationPayload.ActionPayload(
                                NotificationPayload.Actions.APP,
                                null,
                                "Action 1 title"
                            ),
                            NotificationPayload.ActionPayload(
                                NotificationPayload.Actions.DEEPLINK,
                                "http://deeplink?search=something",
                                "Action 2 title"
                            ),
                            NotificationPayload.ActionPayload(
                                NotificationPayload.Actions.BROWSER,
                                "http://google.com?search=something",
                                "Action 3 title"
                            )
                        ),
                        it.buttons
                    )
                    assertEquals(
                        NotificationPayload.ActionPayload(NotificationPayload.Actions.APP),
                        it.notificationAction
                    )
                    assertEquals(
                        NotificationData(
                            eventType = "campaign",
                            campaignId = "5db9ab54b073dfb424ccfa6f",
                            campaignName = "Wassil's push",
                            actionId = 2,
                            actionName = "Unnamed mobile push",
                            actionType = "mobile notification",
                            campaignPolicy = "",
                            subject = "Notification title",
                            platform = "android",
                            language = "",
                            recipient = "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-length
                            campaignData = CampaignData(
                                source = "exponea",
                                campaign = "Testing mobile push",
                                medium = "mobile_push_notification"
                            )
                        ),
                        it.notificationData
                    )
                    assertEquals(
                        hashMapOf(
                            "campaign_name" to "Wassil's push",
                            "event_type" to "campaign",
                            "action_id" to "2",
                            "action_type" to "mobile notification",
                            "campaign_policy" to "",
                            "subject" to "Notification title",
                            "action_name" to "Unnamed mobile push",
                            "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-length
                            "some property" to "some value",
                            "language" to "",
                            "campaign_id" to "5db9ab54b073dfb424ccfa6f",
                            "platform" to "android"
                        ),
                        it.attributes
                    )
                }
            ),
            TestCase(
                "silent notification",
                SILENT_NOTIFICATION,
                {
                    assertEquals(0, it.notificationId)
                    assertEquals("Silent push", it.title)
                    assertEquals("", it.message)
                    assertEquals(null, it.image)
                    assertEquals(null, it.sound)
                    assertEquals(null, it.buttons)
                    assertEquals(
                        NotificationPayload.ActionPayload(NotificationPayload.Actions.APP),
                        it.notificationAction
                    )
                    assertEquals(NotificationData(), it.notificationData)
                    assertEquals(mapOf("silent_test" to "value"), it.attributes)
                }
            )
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
        mockkConstructor(Date::class)
        every { anyConstructed<Date>().time } returns 10 * 1000 // mock current time
    }

    @After
    fun unmock() {
        // mockk has a problem when it sometimes throws an exception, in that case just try again
        try { unmockkAll() } catch (error: ConcurrentModificationException) { unmock() }
    }

    @Test
    fun parseNotificationPayloadTest() {
        validator(NotificationPayload(notificationPayload))
    }
}
