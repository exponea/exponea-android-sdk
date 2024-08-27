package com.exponea.sdk.manager

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.repository.ExponeaConfigRepository
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.repository.PushTokenRepositoryProvider
import com.exponea.sdk.services.ExponeaPushTrackingActivity
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
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class FcmManagerImplTrackingTest(
    @Suppress("UNUSED_PARAMETER")
    name: String,
    private val deliveredTimestamp: Double,
    private val clickedTimestamp: Double,
    private val intentGetter: (notification: Notification) -> Intent

) : ExponeaSDKTest() {
    private lateinit var manager: FcmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var context: Context
    private lateinit var trackingConsentManager: TrackingConsentManager

    companion object {
        data class TestCase(
            val name: String,
            val deliveredTimestamp: Double,
            val clickedTimestamp: Double,
            val intentGetter: (notification: Notification) -> Intent
        )

        private const val SENT_TIMESTAMP = 1614585422.20

        private val testCases = arrayListOf(
            TestCase(
                name = "sent -> delivered -> clicked",
                deliveredTimestamp = SENT_TIMESTAMP + 2,
                clickedTimestamp = SENT_TIMESTAMP + 4,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "sent -> clicked -> delivered",
                deliveredTimestamp = SENT_TIMESTAMP + 4,
                clickedTimestamp = SENT_TIMESTAMP + 2,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "delivered -> sent -> clicked",
                deliveredTimestamp = SENT_TIMESTAMP - 2,
                clickedTimestamp = SENT_TIMESTAMP + 2,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "clicked -> sent -> delivered",
                deliveredTimestamp = SENT_TIMESTAMP + 2,
                clickedTimestamp = SENT_TIMESTAMP - 2,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "delivered -> clicked -> sent",
                deliveredTimestamp = SENT_TIMESTAMP - 4,
                clickedTimestamp = SENT_TIMESTAMP - 2,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "clicked -> delivered -> sent",
                deliveredTimestamp = SENT_TIMESTAMP - 2,
                clickedTimestamp = SENT_TIMESTAMP - 4,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "clicked == delivered == sent",
                deliveredTimestamp = SENT_TIMESTAMP,
                clickedTimestamp = SENT_TIMESTAMP,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            )
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Handling {0}")
        fun data(): List<Array<out Any?>> {
            return testCases.map {
                arrayOf(
                    it.name,
                    it.deliveredTimestamp,
                    it.clickedTimestamp,
                    it.intentGetter
                )
            }
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        Exponea.flushMode = FlushMode.MANUAL
        ExponeaConfigRepository.set(context, ExponeaConfiguration())
        trackingConsentManager = mockkClass(TrackingConsentManagerImpl::class)
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
            PushNotificationRepositoryImpl(ExponeaPreferencesImpl(context)),
            trackingConsentManager
        )
        notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        mockkObject(Exponea)
        every { Exponea.trackDeliveredPush(any(), any()) } just Runs
        every { Exponea.trackClickedPush(any(), any(), any()) } just Runs
    }

    @Test
    fun `should track delivered and clicked events with correct timestamps`() {
        val notification = NotificationTestPayloads.PRODUCTION_NOTIFICATION
        val deliveredTimestampSlot = slot<Double>()
        val clickedTimestampSlot = slot<Double>()
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs

        manager.handleRemoteMessage(notification, notificationManager, true, timestamp = deliveredTimestamp)
        verify(exactly = 1) {
            trackingConsentManager.trackDeliveredPush(any(), capture(deliveredTimestampSlot), any(), any(), any())
        }
        ExponeaPushTrackingActivity().processPushClick(
            context,
            intentGetter(notificationSlot.captured),
            clickedTimestamp
        )
        verify(exactly = 1) {
            Exponea.trackClickedPush(any(), any(), capture(clickedTimestampSlot))
        }
        assertTrue { clickedTimestampSlot.captured > deliveredTimestampSlot.captured }
        assertTrue { deliveredTimestampSlot.captured > SENT_TIMESTAMP }
    }

    @Test
    fun `should track delivered and clicked events correctly when send_timestamp is missing`() {
        val notification = NotificationTestPayloads.PRODUCTION_NOTIFICATION_WITHOUT_SENT_TIME_AND_TYPE
        val deliveredTimestampSlot = slot<Double>()
        val clickedTimestampSlot = slot<Double>()
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs

        manager.handleRemoteMessage(notification, notificationManager, true, timestamp = deliveredTimestamp)
        verify(exactly = 1) {
            trackingConsentManager.trackDeliveredPush(any(), capture(deliveredTimestampSlot), any(), any(), any())
        }
        ExponeaPushTrackingActivity().processPushClick(
            context,
            intentGetter(notificationSlot.captured),
            clickedTimestamp
        )
        verify(exactly = 1) {
            Exponea.trackClickedPush(any(), any(), capture(clickedTimestampSlot))
        }
        // when set_timestamp is not sent from server, we can not guarantee
        // sent -> delivered -> clicked order, only delivered -> clicked order
        assertTrue { clickedTimestampSlot.captured > deliveredTimestampSlot.captured }
    }

    @Test
    fun `should NOT track delivered and clicked events when consent is not given`() {
        val notification = NotificationTestPayloads.NOTIFICATION_WITH_DENIED_CONSENT
        val deliveredTimestampSlot = slot<Double>()
        val clickedTimestampSlot = slot<Double>()
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs

        manager.handleRemoteMessage(notification, notificationManager, true, timestamp = deliveredTimestamp)
        verify(exactly = 0) {
            trackingConsentManager.trackDeliveredPush(any(), capture(deliveredTimestampSlot), any(), any(), any())
        }
        ExponeaPushTrackingActivity().processPushClick(
            context,
            intentGetter(notificationSlot.captured),
            clickedTimestamp
        )
        verify(exactly = 0) {
            Exponea.trackClickedPush(any(), any(), capture(clickedTimestampSlot))
        }
    }

    @Test
    fun `should NOT track delivered event and DO track clicked event when consent is not given but action forced`() {
        val notification = NotificationTestPayloads.NOTIFICATION_WITH_DENIED_CONSENT_BUT_ACTION_FORCED
        val deliveredTimestampSlot = slot<Double>()
        val clickedTimestampSlot = slot<Double>()
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs

        manager.handleRemoteMessage(notification, notificationManager, true, timestamp = deliveredTimestamp)
        verify(exactly = 0) {
            trackingConsentManager.trackDeliveredPush(any(), capture(deliveredTimestampSlot), any(), any(), any())
        }
        ExponeaPushTrackingActivity().processPushClick(
            context,
            intentGetter(notificationSlot.captured),
            clickedTimestamp
        )
        verify(exactly = 1) {
            Exponea.trackClickedPush(any(), any(), capture(clickedTimestampSlot))
        }
    }
}
