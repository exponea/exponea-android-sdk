package com.exponea.sdk.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.manager.FcmManager
import com.exponea.sdk.manager.FcmManagerImpl
import com.exponea.sdk.manager.TrackingConsentManagerImpl
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.data.NotificationTestPayloads
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.util.Date
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class ExponeaPushTrackingActivityNotificationsTest(
    @Suppress("UNUSED_PARAMETER")
    name: String,
    private val notificationPayload: Map<String, String>,
    private val intentGetter: (notification: Notification) -> Intent,
    private val expectedTrackingData: NotificationData?,
    private val expectedTrackingActionData: NotificationAction?
) : ExponeaSDKTest() {
    private lateinit var manager: FcmManager
    private lateinit var notificationManager: NotificationManager

    companion object {
        data class TestCase(
            val name: String,
            val notificationPayload: Map<String, String>,
            val intentGetter: (notification: Notification) -> Intent,
            val expectedTrackingData: NotificationData?,
            val expectedTrackingActionData: NotificationAction?
        )

        private val testCases = arrayListOf(
            TestCase(
                name = "basic notification",
                notificationPayload = NotificationTestPayloads.BASIC_NOTIFICATION,
                intentGetter = { shadowOf(it.contentIntent).savedIntent },
                expectedTrackingData = NotificationData(),
                expectedTrackingActionData = NotificationAction("notification", null, null)
            ),

            TestCase(
                name = "notification with deeplink",
                notificationPayload = NotificationTestPayloads.DEEPLINK_NOTIFICATION,
                intentGetter = { shadowOf(it.contentIntent).savedIntent },
                expectedTrackingData = NotificationData(),
                expectedTrackingActionData = NotificationAction("notification", null, "app://test")
            ),

            TestCase(
                name = "notification with web url",
                notificationPayload = NotificationTestPayloads.BROWSER_NOTIFICATION,
                intentGetter = { shadowOf(it.contentIntent).savedIntent },
                expectedTrackingData = NotificationData(),
                expectedTrackingActionData = NotificationAction("notification", null, "http://google.com")
            ),

            TestCase(
                name = "notification with actions",
                notificationPayload = NotificationTestPayloads.ACTIONS_NOTIFICATION,
                intentGetter = { shadowOf(it.contentIntent).savedIntent },
                expectedTrackingData = NotificationData(),
                expectedTrackingActionData = NotificationAction("notification", null, null)
            ),

            TestCase(
                name = "notification with actions using first action",
                notificationPayload = NotificationTestPayloads.ACTIONS_NOTIFICATION,
                intentGetter = { shadowOf(it.actions[0].actionIntent).savedIntent },
                expectedTrackingData = NotificationData(),
                expectedTrackingActionData = NotificationAction("button", "Action 1 title", null)
            ),

            TestCase(
                name = "notification with actions using second action",
                notificationPayload = NotificationTestPayloads.ACTIONS_NOTIFICATION,
                intentGetter = { shadowOf(it.actions[1].actionIntent).savedIntent },
                expectedTrackingData = NotificationData(),
                expectedTrackingActionData = NotificationAction("button", "Action 2 title", "app://deeplink")
            ),

            TestCase(
                name = "notification with actions using third action",
                notificationPayload = NotificationTestPayloads.ACTIONS_NOTIFICATION,
                intentGetter = { shadowOf(it.actions[2].actionIntent).savedIntent },
                expectedTrackingData = NotificationData(),
                expectedTrackingActionData = NotificationAction("button", "Action 3 title", "http://google.com")
            ),

            TestCase(
                "notification from production",
                NotificationTestPayloads.PRODUCTION_NOTIFICATION,
                intentGetter = { shadowOf(it.contentIntent).savedIntent },
                expectedTrackingData = NotificationData(
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
                expectedTrackingActionData = NotificationAction("notification", null, null)
            ),

            TestCase(
                "notification without consent",
                NotificationTestPayloads.NOTIFICATION_WITH_DENIED_CONSENT,
                intentGetter = { shadowOf(it.contentIntent).savedIntent },
                expectedTrackingData = NotificationData(
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
                    hasTrackingConsent = false
                ),
                expectedTrackingActionData = NotificationAction("notification", null, null)
            )
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Opening {0}")
        fun data(): List<Array<out Any?>> {
            return testCases.map {
                arrayOf(
                    it.name,
                    it.notificationPayload,
                    it.intentGetter,
                    it.expectedTrackingData,
                    it.expectedTrackingActionData
                )
            }
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        Exponea.flushMode = FlushMode.MANUAL
        ExponeaConfigRepository.set(context, ExponeaConfiguration())
        val trackingConsentManager = mockkClass(TrackingConsentManagerImpl::class)
        every {
            trackingConsentManager.trackDeliveredPush(any(), any(), any(), any(), any())
        } just Runs
        every {
            trackingConsentManager.trackClickedPush(any(), any(), any(), any())
        } just Runs
        manager = FcmManagerImpl(
            context,
            ExponeaConfiguration(),
            mockkClass(EventManagerImpl::class),
            PushTokenRepositoryProvider.get(context),
            trackingConsentManager
        )
        notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        mockkObject(Exponea)
        every { Exponea.trackDeliveredPush(any(), any()) } just Runs
        every { Exponea.trackClickedPush(any(), any(), any()) } just Runs
        mockkConstructorFix(Date::class)
    }

    @Test
    fun onReceiveTest() {
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs
        val expectedCreationTime = expectedTrackingData!!.campaignData.createdAt * 1000
        every { anyConstructed<Date>().time } returns expectedCreationTime.toLong() // mock current time
        manager.handleRemoteMessage(notificationPayload, notificationManager, true)

        ExponeaPushTrackingActivity().processPushClick(
            ApplicationProvider.getApplicationContext(),
            intentGetter(notificationSlot.captured))
        verify(exactly = if (expectedTrackingData.hasTrackingConsent) 1 else 0) {
            Exponea.trackClickedPush(expectedTrackingData, expectedTrackingActionData, any())
        }
    }
}
