package com.exponea.sdk.models

import android.os.Parcel
import com.exponea.sdk.testutil.ExponeaSDKTest
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
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

    @Suppress("UNCHECKED_CAST")
    private fun getNotificationDataWithNulls() =
        NotificationData(
            dataMap = hashMapOf(
                "event_type" to "mock event type",
                "campaign_id" to "mock campaign id",
                "campaign_name" to null,
                "product_list" to arrayListOf(
                    hashMapOf(
                        "item_id" to "1234",
                        "item_quantity" to null
                    )
                ),
                "product_ids" to arrayListOf("1234", null, "6789"),
                "push_content" to hashMapOf(
                    "title" to null,
                    "actions" to arrayListOf(
                        hashMapOf(
                            "title" to null,
                            "action" to "app"
                        )
                    ),
                    "message" to null
                )
            ) as HashMap<String, Any>,
            campaignMap = mapOf(
                "utm_source" to "mock source",
                "utm_campaign" to null
            ) as Map<String, String>,
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

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `should serialize - deserialize data with NULL to parcel`() {
        val notificationData = getNotificationDataWithNulls()
        val parcel = Parcel.obtain()
        notificationData.writeToParcel(parcel, notificationData.describeContents())
        parcel.setDataPosition(0)
        val createdFromParcel = NotificationData.createFromParcel(parcel)
        // createdFromParcel will not equals due to missing NULL values
        assertEquals(notificationData.attributes["event_type"], createdFromParcel.attributes["event_type"])
        assertEquals(notificationData.attributes["campaign_id"], createdFromParcel.attributes["campaign_id"])
        assertEquals(
            (notificationData.attributes["product_list"] as List<Map<String, *>>)[0]["item_id"],
            (createdFromParcel.attributes["product_list"] as List<Map<String, *>>)[0]["item_id"]
        )
        assertEquals(
            (notificationData.attributes["product_ids"] as ArrayList<*>)[0],
            (createdFromParcel.attributes["product_ids"] as ArrayList<*>)[0]
        )
        assertEquals(
            (notificationData.attributes["product_ids"] as ArrayList<*>)[2],
            (createdFromParcel.attributes["product_ids"] as ArrayList<*>)[2]
        )
        assertEquals(1, (createdFromParcel.attributes["push_content"] as Map<String, *>).size)
        val pushContentActions =
            (createdFromParcel.attributes["push_content"] as Map<String, *>)["actions"] as List<Map<String, String>>
        assertEquals("app", pushContentActions[0]["action"])
        assertEquals(notificationData.campaignData.source, createdFromParcel.campaignData.source)
        assertNull(createdFromParcel.consentCategoryTracking)
        assertTrue(createdFromParcel.hasTrackingConsent)
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
