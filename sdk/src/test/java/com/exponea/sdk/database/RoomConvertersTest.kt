package com.exponea.sdk.database

import com.exponea.sdk.models.ExponeaProject
import com.exponea.sdk.testutil.data.NotificationTestPayloads
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RoomConvertersTest {

    private val converters = Converters()
    private val mockAnyMap = hashMapOf(
        "campaign_name" to "Wassil's push",
        "event_type" to "campaign",
        "action_id" to 2.0,
        "action_type" to "mobile notification",
        "campaign_policy" to "",
        "subject" to "Notification title",
        "action_name" to "Unnamed mobile push",
        "recipient" to "eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2a",
        "campaign_id" to "5db9ab54b073dfb424ccfa6f",
        "platform" to "android",
        "first_level_attribute" to hashMapOf(
            "second_level__nested_attribute" to hashMapOf(
                "third_level_attribute" to "third_level_value"
            ),
            "second_level_attribute" to "second_level_value"
        ),
        "product_list" to arrayListOf(
            hashMapOf(
                "item_id" to "1234",
                "item_quantity" to 3.0
            ),
            hashMapOf(
                "item_id" to "2345",
                "item_quantity" to 2.0
            ),
            hashMapOf(
                "item_id" to "6789",
                "item_quantity" to 1.0
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
    )

    @Test
    fun `should convert project  correctly`() {
        val project = ExponeaProject("mock-url", "mock-token", "mock-autorization")
        val projectString = converters.fromProject(project)
        assertEquals(converters.toProject(projectString), project)
    }

    @Test
    fun `should convert project with empty fields correctly`() {
        val project = ExponeaProject("", "", "")
        val projectString = converters.fromProject(project)
        assertEquals(converters.toProject(projectString), project)
    }

    @Test
    fun `should convert string map correctly`() {
        // use notification payload as mock hashMap
        val payload = NotificationTestPayloads.NOTIFICATION_WITH_NESTED_ATTRIBUTES
        val mapString = converters.fromStringMap(payload)
        assertEquals(converters.toStringMap(mapString), payload)
    }

    @Test
    fun `should convert any map correctly`() {
        val mapString = converters.fromAnyMap(mockAnyMap as HashMap<String, Any>)
        assertEquals(mockAnyMap, converters.toAnyMap(mapString))
    }
}
