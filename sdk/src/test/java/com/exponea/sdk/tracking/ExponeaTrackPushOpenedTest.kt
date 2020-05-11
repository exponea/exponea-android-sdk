package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.ExportedEventType
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.componentForTesting
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
internal class ExponeaTrackPushOpenedTest(
    private val name: String,
    private val notificationData: NotificationData?,
    private val actionData: NotificationAction?,
    private val eventName: String,
    private val eventProperties: HashMap<String, Any>,
    private val eventType: EventType
) : ExponeaSDKTest() {
    companion object {
        data class TestCase(
            val name: String,
            val notificationData: NotificationData?,
            val actionData: NotificationAction?,
            val eventName: String,
            val eventProperties: HashMap<String, Any>,
            val eventType: EventType
        )

        private val testCases = arrayListOf(
            TestCase(
                "empty push data",
                null,
                null,
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "clicked",
                    "platform" to "android",
                    "cta" to "notification",
                    "url" to "app"
                ),
                EventType.PUSH_OPENED
            ),
            TestCase(
                "custom event type",
                NotificationData(eventType = "my_push_event"),
                null,
                "my_push_event",
                hashMapOf(
                    "status" to "clicked",
                    "platform" to "android",
                    "cta" to "notification",
                    "url" to "app"
                ),
                EventType.TRACK_EVENT
            ),
            TestCase(
                "empty event type",
                NotificationData(eventType = ""),
                null,
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "clicked",
                    "platform" to "android",
                    "cta" to "notification",
                    "url" to "app"
                ),
                EventType.PUSH_OPENED
            ),
            TestCase(
                "custom action name",
                null,
                NotificationAction(actionType = "mock type", actionName = "my action name"),
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "clicked",
                    "platform" to "android",
                    "cta" to "my action name",
                    "url" to "app"
                ),
                EventType.PUSH_OPENED
            ),
            TestCase(
                "custom action url",
                null,
                NotificationAction(actionType = "mock type", url = "my action url"),
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "clicked",
                    "platform" to "android",
                    "cta" to "notification",
                    "url" to "my action url"
                ),
                EventType.PUSH_OPENED
            ),
            TestCase(
                "custom platform",
                NotificationData(platform = "custom platform"),
                null,
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "clicked",
                    "platform" to "custom platform",
                    "cta" to "notification",
                    "url" to "app"
                ),
                EventType.PUSH_OPENED
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
                NotificationAction("mock action type", "mock action name", "mock action url"),
                Constants.EventTypes.push,
                hashMapOf(
                    "status" to "clicked",
                    "cta" to "mock action name",
                    "url" to "mock action url",
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
                EventType.PUSH_OPENED
            )
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Tracking {0}")
        fun data(): List<Array<out Any?>> {
            return testCases.map {
                arrayOf(
                    it.name,
                    it.notificationData,
                    it.actionData,
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

    @After
    fun after() {
        unmockkAll()
    }

    @Test
    fun `should track push opened`() {
        val eventSlot = slot<ExportedEventType>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot))
        } just Runs
        Exponea.trackClickedPush(notificationData, actionData)
        assertEquals(notificationData?.campaignData, Exponea.componentForTesting.campaignRepository.get())
        assertEquals(eventName, eventSlot.captured.type)
        assertEquals(eventProperties, eventSlot.captured.properties)
        assertEquals(eventType, eventTypeSlot.captured)
    }
}
