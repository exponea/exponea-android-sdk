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
import com.exponea.sdk.repository.FirebaseTokenRepositoryImpl
import com.exponea.sdk.repository.PushNotificationRepositoryImpl
import com.exponea.sdk.services.ExponeaPushReceiver
import com.exponea.sdk.testutil.ExponeaSDKTest
import com.exponea.sdk.testutil.data.NotificationTestPayloads
import com.google.firebase.messaging.RemoteMessage
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
                deliveredTimestamp = 1614585424.20,
                clickedTimestamp = 1614585426.20,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "sent -> clicked -> delivered",
                deliveredTimestamp = 1614585426.20,
                clickedTimestamp = 1614585424.20,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "delivered -> sent -> clicked",
                deliveredTimestamp = 1614585420.20,
                clickedTimestamp = 1614585424.20,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "clicked -> sent -> delivered",
                deliveredTimestamp = 1614585424.20,
                clickedTimestamp = 1614585420.20,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "delivered -> clicked -> sent",
                deliveredTimestamp = 1614585418.20,
                clickedTimestamp = 1614585420.20,
                intentGetter = { Shadows.shadowOf(it.contentIntent).savedIntent }
            ),
            TestCase(
                name = "clicked -> delivered -> sent",
                deliveredTimestamp = 1614585420.20,
                clickedTimestamp = 1614585418.20,
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
        manager = FcmManagerImpl(
            context,
            ExponeaConfiguration(),
            mockkClass(EventManagerImpl::class),
            FirebaseTokenRepositoryImpl(ExponeaPreferencesImpl(context)),
            PushNotificationRepositoryImpl(ExponeaPreferencesImpl(context))
        )
        notificationManager = spyk(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        mockkObject(Exponea)
        every { Exponea.trackDeliveredPush(any(), any()) } just Runs
        every { Exponea.trackClickedPush(any(), any(), any()) } just Runs
    }

    @Test
    fun `should track delivered and clicked events with correct timestamps`() {
        val notification = RemoteMessage.Builder("1")
            .setData(NotificationTestPayloads.PRODUCTION_NOTIFICATION)
            .build()
        val deliveredTimestampSlot = slot<Double>()
        val clickedTimestampSlot = slot<Double>()
        val notificationSlot = slot<Notification>()
        every { notificationManager.notify(any(), capture(notificationSlot)) } just Runs

        manager.handleRemoteMessage(notification, notificationManager, true, timestamp = deliveredTimestamp)
        verify(exactly = 1) {
            Exponea.trackDeliveredPush(any(), capture(deliveredTimestampSlot))
        }
        ExponeaPushReceiver().onReceiveUnsafe(
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
}
