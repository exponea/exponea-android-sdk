package com.exponea.sdk.models

import com.exponea.sdk.models.eventfilter.EventFilter
import com.google.gson.Gson
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockWebServer
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
                "event_type": "session_start",
                "filter": []
            },
            "date_filter": {
                "enabled": false,
                "from_date": null,
                "to_date": null
            },
            "load_delay": 2000,
            "close_timeout": 1000,
            "payload_html": null,
            "is_html": false
        }
        """

        fun getInAppMessage(
            id: String? = null,
            dateFilter: DateFilter? = null,
            trigger: EventFilter? = null,
            frequency: String? = null,
            imageUrl: String? = null,
            priority: Int? = null,
            timeout: Long? = null,
            delay: Long? = null,
            environment: MockWebServer? = null
        ): InAppMessage {
            return InAppMessage(
                id = id ?: "5dd86f44511946ea55132f29",
                name = "Test serving in-app message",
                rawMessageType = "modal",
                rawFrequency = frequency ?: "unknown",
                variantId = 0,
                variantName = "Variant A",
                trigger = trigger ?: EventFilter("session_start", arrayListOf()),
                dateFilter = dateFilter ?: DateFilter(false, null, null),
                priority = priority,
                delay = delay,
                timeout = timeout,
                payload = InAppMessagePayload(
                    imageUrl = testUrl(imageUrl ?: "https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg", environment),
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
                            buttonLink = testUrl("https://someaddress.com", environment),
                            buttonTextColor = "#ffffff",
                            buttonBackgroundColor = "#f44cac"
                        )
                    )
                ),
                payloadHtml = null,
                isHtml = false,
                consentCategoryTracking = null,
                rawHasTrackingConsent = null
            )
        }

        private fun testUrl(url: String, environment: MockWebServer?): String {
            if (environment == null) {
                return url
            }
            return environment.url(url).toString()
        }

        fun getInAppMessage(
            id: String? = null,
            dateFilter: DateFilter? = null,
            trigger: EventFilter? = null,
            frequency: String? = null,
            payload: InAppMessagePayload? = null,
            payloadHtml: String? = null,
            variantId: Int = 0,
            variantName: String? = null,
            priority: Int? = null,
            timeout: Long? = null,
            delay: Long? = null
        ): InAppMessage {
            return InAppMessage(
                id = id ?: "5dd86f44511946ea55132f29",
                name = "Test serving in-app message",
                rawMessageType = "modal",
                rawFrequency = frequency ?: "unknown",
                variantId = variantId,
                variantName = variantName ?: "Variant A",
                trigger = trigger ?: EventFilter("session_start", arrayListOf()),
                dateFilter = dateFilter ?: DateFilter(false, null, null),
                priority = priority,
                delay = delay,
                timeout = timeout,
                payload = payload,
                payloadHtml = payloadHtml,
                isHtml = payloadHtml?.isNotBlank(),
                consentCategoryTracking = null,
                rawHasTrackingConsent = null
            )
        }
    }

    @Test
    fun `should parse in-app message from json`() {
        assertEquals(getInAppMessage(timeout = 1000, delay = 2000), Gson().fromJson(json, InAppMessage::class.java))
    }
}
