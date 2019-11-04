package com.exponea.sdk.models

import android.os.Parcel
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NotificationDataTest : ExponeaSDKTest() {
    private fun getMockNotificationData() =
        NotificationData(
            eventType = "mock event type",
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
        )

    @Test
    fun `should serialize - deserialize empty data to parcel`() {
        val notificationData = NotificationData()
        val parcel = Parcel.obtain()
        notificationData.writeToParcel(parcel, notificationData.describeContents())
        parcel.setDataPosition(0)

        assertEquals(notificationData, NotificationData.createFromParcel(parcel))
    }

    @Test
    fun `should serialize - deserialize sample data to parcel`() {
        val notificationData = getMockNotificationData()
        val parcel = Parcel.obtain()
        notificationData.writeToParcel(parcel, notificationData.describeContents())
        parcel.setDataPosition(0)

        assertEquals(notificationData, NotificationData.createFromParcel(parcel))
    }

    @Test
    fun `should serialize empty data for tracking`() {
        assertEquals(
            hashMapOf(
                "campaign_id" to null,
                "campaign_name" to null,
                "action_id" to null,
                "action_name" to null,
                "action_type" to null,
                "campaign_policy" to null,
                "platform" to null,
                "language" to null,
                "recipient" to null,
                "subject" to null
            ),
            NotificationData().getTrackingData()
        )
    }

    @Test
    fun `should serialize mock data for tracking`() {
        assertEquals(
            hashMapOf(
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
            getMockNotificationData().getTrackingData()
        )
    }
}