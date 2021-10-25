package com.exponea.sdk.tracking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.manager.EventManagerImpl
import com.exponea.sdk.models.Event
import com.exponea.sdk.models.EventType
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.FlushMode
import com.exponea.sdk.models.PurchasedItem
import com.exponea.sdk.testutil.ExponeaSDKTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExponeaTrackVirtualPaymentTest : ExponeaSDKTest() {
    val purchase = PurchasedItem(
        currency = "USD",
        value = 200.3,
        productId = "Item",
        productTitle = "Speed Boost",
        paymentSystem = "payment-system"
    )

    @Before
    fun before() {
        mockkConstructor(EventManagerImpl::class)
        skipInstallEvent()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration = ExponeaConfiguration(projectToken = "mock-token", automaticSessionTracking = false)
        Exponea.flushMode = FlushMode.MANUAL
        Exponea.init(context, configuration)
    }

    @Test
    fun `should track virtual payment`() {
        val eventSlot = slot<Event>()
        val eventTypeSlot = slot<EventType>()
        every {
            anyConstructed<EventManagerImpl>().addEventToQueue(capture(eventSlot), capture(eventTypeSlot))
        } just Runs
        Exponea.trackPaymentEvent(purchasedItem = purchase)
        verify(exactly = 1) {
            anyConstructed<EventManagerImpl>().addEventToQueue(any(), any())
        }

        assertEquals("payment", eventSlot.captured.type)
        assertTrue(
            eventSlot.captured.properties?.entries?.containsAll<Map.Entry<String, Any>>(
                hashMapOf(
                    "currency" to "USD",
                    "brutto" to 200.3,
                    "item_id" to "Item",
                    "product_title" to "Speed Boost",
                    "payment_system" to "payment-system"
                ).entries
            ) ?: false
        )
        assertEquals(EventType.PAYMENT, eventTypeSlot.captured)
    }
}
