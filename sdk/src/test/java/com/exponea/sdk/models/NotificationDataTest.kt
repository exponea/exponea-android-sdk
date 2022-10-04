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
        NotificationData(dataMap = hashMapOf(
            "event_type" to "mock event type",
            "campaign_id" to "mock campaign id",
            "campaign_name" to "mock campaign name",
            "action_id" to 123456,
            "action_name" to "mock action name",
            "action_type" to "mock action type",
            "campaign_policy" to "mock campaign policy",
            "platform" to "mock platform",
            "language" to "mock language",
            "recipient" to "mock recipient",
            "subject" to "mock title",
            "sent_timestamp" to 1614585422.20,
            "type" to "push",
            "product_list" to arrayListOf(
                hashMapOf(
                        "item_id" to "1234",
                        "item_quantity" to 3
                ),
                hashMapOf(
                        "item_id" to "2345",
                        "item_quantity" to 2
                ),
                hashMapOf(
                        "item_id" to "6789",
                        "item_quantity" to 1
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
                        "utm_source" to "mock source",
                        "utm_campaign" to "mock campaign",
                        "utm_medium" to "mock medium",
                        "utm_content" to "mock content",
                        "utm_term" to "mock term"
                ),
            consentCategoryTracking = null,
            hasTrackingConsent = true
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
            hashMapOf(),
            NotificationData().getTrackingData()
        )
    }

    @Test
    fun `should serialize mock data for tracking`() {
        assertEquals(
            hashMapOf(
                "campaign_id" to "mock campaign id",
                "campaign_name" to "mock campaign name",
                "action_id" to 123456,
                "action_name" to "mock action name",
                "action_type" to "mock action type",
                "campaign_policy" to "mock campaign policy",
                "platform" to "mock platform",
                "language" to "mock language",
                "recipient" to "mock recipient",
                "subject" to "mock title",
                "utm_source" to "mock source",
                "utm_medium" to "mock medium",
                "utm_campaign" to "mock campaign",
                "utm_content" to "mock content",
                "utm_term" to "mock term",
                "sent_timestamp" to 1614585422.20,
                "type" to "push",
                "product_list" to arrayListOf(
                    hashMapOf(
                            "item_id" to "1234",
                            "item_quantity" to 3
                    ),
                    hashMapOf(
                            "item_id" to "2345",
                            "item_quantity" to 2
                    ),
                    hashMapOf(
                            "item_id" to "6789",
                            "item_quantity" to 1
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
            getMockNotificationData().getTrackingData()
        )
    }
}
