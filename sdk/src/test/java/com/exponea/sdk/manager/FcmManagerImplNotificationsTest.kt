package com.exponea.sdk.manager

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CampaignData
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationAction.Companion.ACTION_TYPE_BUTTON
import com.exponea.sdk.models.NotificationAction.Companion.ACTION_TYPE_NOTIFICATION
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.FirebaseTokenRepositoryImpl
import com.exponea.sdk.repository.PushNotificationRepository
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.data.NotificationTestPayloads
import com.google.firebase.messaging.RemoteMessage
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
    private val expectedNotificationData: Map<String, String>?
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
            val expectedNotificationData: Map<String, String>? = null
        )

        private fun validateIntent(
            intent: Intent,
            expectedAction: String,
            expectedNotificationAction: NotificationAction,
            expectedNotificationData: NotificationData?,
            expectedPayload: HashMap<String, String>
        ) {
            assertEquals(expectedAction, intent.action)
            assertEquals(expectedNotificationAction, intent.extras?.get(ExponeaPushReceiver.EXTRA_ACTION_INFO))
            assertEquals(expectedNotificationData, intent.extras?.get(ExponeaPushReceiver.EXTRA_DATA))
            assertEquals(expectedPayload, intent.extras?.get(ExponeaPushReceiver.EXTRA_CUSTOM_DATA))
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
                        ExponeaPushReceiver.ACTION_CLICKED,
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
                        ExponeaPushReceiver.ACTION_DEEPLINK_CLICKED,
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
                        ExponeaPushReceiver.ACTION_URL_CLICKED,
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
                        ExponeaPushReceiver.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_NOTIFICATION),
                        NotificationData(),
                        NotificationTestPayloads.ACTIONS_NOTIFICATION
                    )

                    assertEquals(3, it.actions.size)
                    assertEquals("Action 1 title", it.actions[0].title)
                    validateIntent(
                        shadowOf(it.actions[0].actionIntent).savedIntent,
                        ExponeaPushReceiver.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 1 title"),
                        NotificationData(),
                        NotificationTestPayloads.ACTIONS_NOTIFICATION
                    )
                    assertEquals("Action 2 title", it.actions[1].title)
                    validateIntent(
                        shadowOf(it.actions[1].actionIntent).savedIntent,
                        ExponeaPushReceiver.ACTION_DEEPLINK_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 2 title", "app://deeplink"),
                        NotificationData(),
                        NotificationTestPayloads.ACTIONS_NOTIFICATION
                    )
                    assertEquals("Action 3 title", it.actions[2].title)
                    validateIntent(
                        shadowOf(it.actions[2].actionIntent).savedIntent,
                        ExponeaPushReceiver.ACTION_URL_CLICKED,
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
                        eventType = "campaign",
                        campaignId = "5db9ab54b073dfb424ccfa6f",
                        campaignName = "Wassil's push",
                        actionId = 2,
                        actionName = "Unnamed mobile push",
                        actionType = "mobile notification",
                        campaignPolicy = "",
                        platform = "android",
                        language = "",
                        subject = "Notification title",
                        recipient = "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-length
                        campaignData = CampaignData(
                            source = "exponea",
                            campaign = "Testing mobile push",
                            medium = "mobile_push_notification"
                        )
                    )
                    assertEquals("Notification title", shadowOf(it).contentTitle)
                    assertEquals("Notification text", shadowOf(it).contentText)
                    validateIntent(
                        shadowOf(it.contentIntent).savedIntent,
                        ExponeaPushReceiver.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_NOTIFICATION),
                        notificationData,
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION
                    )

                    assertEquals(3, it.actions.size)
                    assertEquals("Action 1 title", it.actions[0].title)
                    validateIntent(
                        shadowOf(it.actions[0].actionIntent).savedIntent,
                        ExponeaPushReceiver.ACTION_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 1 title"),
                        notificationData,
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION
                    )
                    assertEquals("Action 2 title", it.actions[1].title)
                    validateIntent(
                        shadowOf(it.actions[1].actionIntent).savedIntent,
                        ExponeaPushReceiver.ACTION_DEEPLINK_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 2 title", "http://deeplink?search=something"),
                        notificationData,
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION
                    )
                    assertEquals("Action 3 title", it.actions[2].title)
                    validateIntent(
                        shadowOf(it.actions[2].actionIntent).savedIntent,
                        ExponeaPushReceiver.ACTION_URL_CLICKED,
                        NotificationAction(ACTION_TYPE_BUTTON, "Action 3 title", "http://google.com?search=something"),
                        notificationData,
                        NotificationTestPayloads.PRODUCTION_NOTIFICATION
                    )
                },
                NotificationData(
                    eventType = "campaign",
                    campaignId = "5db9ab54b073dfb424ccfa6f",
                    campaignName = "Wassil's push",
                    actionId = 2,
                    actionName = "Unnamed mobile push",
                    actionType = "mobile notification",
                    campaignPolicy = "",
                    platform = "android",
                    language = "",
                    subject = "Notification title",
                    recipient = "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z", // ktlint-disable max-line-length
                    campaignData = CampaignData(
                        source = "exponea",
                        campaign = "Testing mobile push",
                        medium = "mobile_push_notification"
                    )
                ),
                mapOf(
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
                    "platform" to "android",
                    "sent_timestamp" to "1614585422.20"
                )
            ),
            TestCase(
                name = "silent notification",
                notificationPayload = NotificationTestPayloads.SILENT_NOTIFICATION,
                expectNotificationCreated = false,
                expectedNotificationId = 0,
                expectedNotificationMatcher = {},
                expectedTrackingData = NotificationData(),
                expectedNotificationData = mapOf("silent_test" to "value")
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
            FirebaseTokenRepositoryImpl(ExponeaPreferencesImpl(context)),
            pushNotificationRepository
        )
        notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        mockkObject(Exponea)
        every { Exponea.trackDeliveredPush(any(), any()) } just Runs
        mockkConstructor(Date::class)
    }

    private fun getRemoteMessage(notification: Map<String, String>): RemoteMessage {
        val messageBundle = Bundle()
        notification.entries.forEach { messageBundle.putString(it.key, it.value) }
        return RemoteMessage(messageBundle)
    }

    @Test
    fun handleRemoteMessageTest() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        val expectedCreationTime = expectedTrackingData!!.campaignData.createdAt * 1000
        every { anyConstructed<Date>().time } returns expectedCreationTime.toLong() // mock current time
        manager.handleRemoteMessage(getRemoteMessage(notificationPayload), notificationManager, true)

        verify(exactly = 1) {
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
