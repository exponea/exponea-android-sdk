package com.exponea.sdk.models

import com.exponea.sdk.manager.NotificationTestPayloads.ACTIONS_NOTIFICATION
import com.exponea.sdk.manager.NotificationTestPayloads.ATTRIBUTES_NOTIFICATION
import com.exponea.sdk.manager.NotificationTestPayloads.BASIC_NOTIFICATION
import com.exponea.sdk.manager.NotificationTestPayloads.BROWSER_NOTIFICATION
import com.exponea.sdk.manager.NotificationTestPayloads.DEEPLINK_NOTIFICATION
import com.exponea.sdk.testutil.ExponeaSDKTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import kotlin.test.assertEquals

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
                    assertEquals(null, it.notificationData)
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
                    assertEquals(null, it.notificationData)
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
                    assertEquals(null, it.notificationData)
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
                    assertEquals(null, it.notificationData)
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
                    assertEquals(null, it.notificationData)
                    assertEquals(null, it.attributes)
                }
            ),
            TestCase(
                "attributes notification",
                ATTRIBUTES_NOTIFICATION,
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
                    assertEquals(
                        NotificationData("5db1582b1b2be24d0bee4de9", "push with buttons", 2),
                        it.notificationData
                    )
                    assertEquals(
                        hashMapOf(
                            "campaign_name" to "push with buttons",
                            "action_id" to "2",
                            "something_else" to "some other value",
                            "campaign_id" to "5db1582b1b2be24d0bee4de9",
                            "something" to "some value"
                        ),
                        it.attributes
                    )
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

    @Test
    fun parseNotificationPayloadTest() {
        validator(NotificationPayload(notificationPayload))
    }
}