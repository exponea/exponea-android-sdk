package com.exponea.sdk.models

import com.google.gson.Gson
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageTest {
    companion object {
        val json = """
        {
            "id": "5dd86f44511946ea55132f29",
            "name": "Test serving in-app message",
            "message_type": "modal",
            "frequency": "unknown",
            "payload": {
                "image_url":"https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg",
                "title":"filip.vozar@exponea.com",
                "title_text_color":"#000000",
                "title_text_size":"22px",
                "body_text":"This is an example of your in-app message body text.",
                "body_text_color":"#000000",
                "body_text_size":"14px",
                "background_color":"#ffffff",
                "close_button_color":"#ffffff",
                "buttons": [
                    {
                        "button_text":"Action",
                        "button_type":"deep-link",
                        "button_link":"https://someaddress.com",
                        "button_text_color":"#ffffff",
                        "button_background_color":"#f44cac"
                    }
                ]
            },
            "variant_id": 0,
            "variant_name": "Variant A",
            "trigger": {
                "type": "event",
                "event_type": "session_start"
            },
            "date_filter": {
                "enabled": false,
                "from_date": null,
                "to_date": null
            }
        }
        """

        fun getInAppMessage(
            id: String? = null,
            dateFilter: DateFilter? = null,
            trigger: InAppMessageTrigger? = null,
            frequency: String? = null,
            imageUrl: String? = null
        ): InAppMessage {
            return InAppMessage(
                id = id ?: "5dd86f44511946ea55132f29",
                name = "Test serving in-app message",
                rawMessageType = "modal",
                rawFrequency = frequency ?: "unknown",
                variantId = 0,
                variantName = "Variant A",
                trigger = trigger ?: InAppMessageTrigger(type = "event", eventType = "session_start"),
                dateFilter = dateFilter ?: DateFilter(false, null, null),
                payload = InAppMessagePayload(
                    imageUrl = imageUrl ?: "https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg",
                    title = "filip.vozar@exponea.com",
                    titleTextColor = "#000000",
                    titleTextSize = "22px",
                    bodyText = "This is an example of your in-app message body text.",
                    bodyTextColor = "#000000",
                    bodyTextSize = "14px",
                    backgroundColor = "#ffffff",
                    closeButtonColor = "#ffffff",
                    buttons = arrayListOf(
                        InAppMessagePayloadButton(
                            rawButtonType = "deep-link",
                            buttonText = "Action",
                            buttonLink = "https://someaddress.com",
                            buttonTextColor = "#ffffff",
                            buttonBackgroundColor = "#f44cac"
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `should parse in-app message from json`() {
        assertEquals(getInAppMessage(), Gson().fromJson(json, InAppMessage::class.java))
    }
}
