package com.exponea.sdk.models

import com.exponea.sdk.models.InAppMessageType.FREEFORM
import com.exponea.sdk.models.InAppMessageType.MODAL
import com.exponea.sdk.models.eventfilter.EventFilter
import com.exponea.sdk.testutil.assertEqualsJsons
import com.exponea.sdk.util.ExponeaGson
import com.google.gson.Gson
import kotlin.test.assertEquals
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

internal class InAppMessageTest {
    companion object {
        val nonRichStyleJson = """
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
                    },{
                        "button_text":"Cancel",
                        "button_type":"cancel",
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
                "enabled": false
            },
            "load_delay": 2000,
            "close_timeout": 1000,
            "is_html": false,
            "is_rich_text": false
        }
        """.trimIndent()

        const val RICH_STYLE_JSON = """
        {
          "id": "5dd86f44511946ea55132f29",
          "name": "Test serving in-app message",
          "message_type": "modal",
          "frequency": "unknown",
          "payload": {
            "image_url": "https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg",
            "title": "filip.vozar@exponea.com",
            "title_text_color": "#000000",
            "title_text_size": "22px",
            "body_text": "This is an example of your in-app message body text.",
            "body_text_color": "#000000",
            "body_text_size": "14px",
            "buttons": [
              {
                "button_type": "deep-link",
                "button_text": "Action",
                "button_link": "https://someaddress.com",
                "button_background_color": "blue",
                "button_text_color": "#ffffff",
                "button_font_url": "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
                "button_width": "hug",
                "button_corner_radius": "12dp",
                "button_margin": "20px 10px 15px 10px",
                "button_font_size": "24px",
                "button_line_height": "32px",
                "button_padding": "20px 10px 15px 10px",
                "button_font_format": ["bold"],
                "button_border_width": "1px",
                "button_border_color": "black",
                "button_enabled": true
              },
              {
                "button_type": "cancel",
                "button_text": "Cancel",
                "button_background_color": "#f44cac",
                "button_text_color": "#ffffff",
                "button_font_url": "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
                "button_width": "hug",
                "button_corner_radius": "12dp",
                "button_margin": "20px 10px 15px 10px",
                "button_font_size": "24px",
                "button_line_height": "32px",
                "button_padding": "20px 10px 15px 10px",
                "button_font_format": ["bold"],
                "button_border_width": "1px",
                "button_border_color": "black",
                "button_enabled": true
              }
            ],
            "background_color": "#ffffff",
            "close_button_color": "#ffffff",
            "image_size": "auto",
            "image_object_fit": "fill",
            "image_margin": "200 10 10 10",
            "title_font_url": "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
            "title_align": "center",
            "title_format": ["bold"],
            "title_line_height": "32px",
            "title_padding": "200px 10px 15px 10px",
            "body_font_url": "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
            "body_align": "center",
            "body_format": ["bold"],
            "body_line_height": "32px",
            "body_padding": "200px 10px 15px 10px",
            "buttons_align": "center",
            "image_aspect_ratio_width": "16",
            "image_aspect_ratio_height": "9",
            "close_button_background_color": "yellow",
            "close_button_margin": "50px 10px",
            "close_button_enabled": true,
            "overlay_color": "#FF00FF10",
            "image_corner_radius": "10px",
            "image_enabled": true,
            "title_enabled": true,
            "body_enabled": true
          },
          "variant_id": 0,
          "variant_name": "Variant A",
          "trigger": {
            "event_type": "session_start",
            "filter": []
          },
          "date_filter": {
            "enabled": false
          },
          "load_delay": 2000,
          "close_timeout": 1000,
          "is_html": false,
          "is_rich_text": true
        }
        """

        val fullscreenRichstyle = """
        {
            "id": "5dd86f44511946ea55132f29",
            "name": "Test serving in-app message",
            "message_type": "fullscreen",
            "frequency": "unknown",
            "payload": {
                "title": "Book a tour for Antelope Canyon",
                "body_text": "This is an example of your in-app personalization body text.",
                "image_url": "https://asset-templates.exponea.dev/misc/media/canyon/canyon.jpg",
                "title_text_color": "#000000",
                "title_text_size": "22px",
                "body_text_color": "#000000",
                "body_text_size": "14px",
                "background_color": "#ffffff",
                "container_margin": "0px",
                "container_padding": "0px",
                "container_corner_radius": "8px",
                "overlay_color": "rgba(0, 0, 0, 0.6)",
                "buttons_align": "center",
                "text_position": "bottom",
                "image_enabled": true,
                "image_size": "auto",
                "image_margin": "0px",
                "image_corner_radius": "0px",
                "image_aspect_ratio_width": "4",
                "image_aspect_ratio_height": "3",
                "image_object_fit": "cover",
                "image_overlay_enabled": false,
                "title_enabled": true,
                "title_format": ["bold"],
                "title_align": "center",
                "title_line_height": "24px",
                "title_padding": "12px 0px 0px 0px",
                "title_font_url": "",
                "body_enabled": true,
                "body_format": [],
                "body_align": "center",
                "body_line_height": "16px",
                "body_padding": "0px 24px",
                "body_font_url": "",
                "close_button_enabled": true,
                "close_button_margin": "8px 8px 0px 0px",
                "close_button_image_url": "https://www.svgrepo.com/show/503004/close.svg",
                "close_button_background_color": "rgba(250, 250, 250, 0.6)",
                "close_button_color": "#000000",
                "buttons": [
                    {
                        "button_text": "Action",
                        "button_type": "deep-link",
                        "button_link": "https://bloomreach.com",
                        "button_text_color": "#ffffff",
                        "button_background_color": "#019ACE",
                        "button_width": "hug_text",
                        "button_corner_radius": "4px",
                        "button_margin": "16px 0px 0px 0px",
                        "button_has_border": false,
                        "button_font_size": "14px",
                        "button_line_height": "14px",
                        "button_padding": "16px 36px",
                        "button_align": "center",
                        "button_border_color": "#006081",
                        "button_border_width": "2px",
                        "button_font_url": "",
                        "button_enabled": true
                    },
                    {
                        "button_text": "Action",
                        "button_type": "deep-link",
                        "button_link": "https://bloomreach.com",
                        "button_text_color": "#ffffff",
                        "button_background_color": "#019ACE",
                        "button_width": "hug_text",
                        "button_corner_radius": "4px",
                        "button_margin": "16px 0px 0px 0px",
                        "button_has_border": false,
                        "button_font_size": "14px",
                        "button_line_height": "14px",
                        "button_padding": "16px 36px",
                        "button_align": "center",
                        "button_border_color": "#006081",
                        "button_border_width": "2px",
                        "button_font_url": "",
                        "button_enabled": false
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
                "enabled": false
            },
            "load_delay": 2000,
            "close_timeout": 1000,
            "is_html": false
        }
        """.trimIndent()

        val modalRichstyle = """
        {
            "id": "5dd86f44511946ea55132f29",
            "name": "Test serving in-app message",
            "message_type": "modal",
            "frequency": "unknown",
            "payload": {
                "title": "Book a tour for Antelope Canyon",
                "body_text": "This is an example of your in-app personalization body text.",
                "image_url": "https://asset-templates.exponea.dev/misc/media/canyon/canyon.jpg",
                "title_text_color": "#000000",
                "title_text_size": "22px",
                "body_text_color": "#000000",
                "body_text_size": "14px",
                "background_color": "#ffffff",
                "container_margin": "0px",
                "container_padding": "0px",
                "container_corner_radius": "8px",
                "overlay_color": "rgba(0, 0, 0, 0.6)",
                "buttons_align": "center",
                "text_position": "bottom",
                "image_enabled": true,
                "image_size": "auto",
                "image_margin": "0px",
                "image_corner_radius": "0px",
                "image_aspect_ratio_width": "4",
                "image_aspect_ratio_height": "3",
                "image_object_fit": "cover",
                "image_overlay_enabled": false,
                "title_enabled": true,
                "title_format": ["bold"],
                "title_align": "center",
                "title_line_height": "24px",
                "title_padding": "12px 0px 0px 0px",
                "title_font_url": "",
                "body_enabled": true,
                "body_format": [],
                "body_align": "center",
                "body_line_height": "16px",
                "body_padding": "0px 24px",
                "body_font_url": "",
                "close_button_enabled": true,
                "close_button_margin": "8px 8px 0px 0px",
                "close_button_image_url": "https://www.svgrepo.com/show/503004/close.svg",
                "close_button_background_color": "rgba(250, 250, 250, 0.6)",
                "close_button_color": "#000000",
                "buttons": [
                    {
                        "button_text": "Action",
                        "button_type": "deep-link",
                        "button_link": "https://bloomreach.com",
                        "button_text_color": "#ffffff",
                        "button_background_color": "#019ACE",
                        "button_width": "hug_text",
                        "button_corner_radius": "4px",
                        "button_margin": "16px 0px 0px 0px",
                        "button_has_border": false,
                        "button_font_size": "14px",
                        "button_line_height": "14px",
                        "button_padding": "16px 36px",
                        "button_align": "center",
                        "button_border_color": "#006081",
                        "button_border_width": "2px",
                        "button_font_url": "",
                        "button_enabled": true
                    },
                    {
                        "button_text": "Action",
                        "button_type": "deep-link",
                        "button_link": "https://bloomreach.com",
                        "button_text_color": "#ffffff",
                        "button_background_color": "#019ACE",
                        "button_width": "hug_text",
                        "button_corner_radius": "4px",
                        "button_margin": "16px 0px 0px 0px",
                        "button_has_border": false,
                        "button_font_size": "14px",
                        "button_line_height": "14px",
                        "button_padding": "16px 36px",
                        "button_align": "center",
                        "button_border_color": "#006081",
                        "button_border_width": "2px",
                        "button_font_url": "",
                        "button_enabled": false
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
                "enabled": false
            },
            "load_delay": 2000,
            "close_timeout": 1000,
            "is_html": false
        }
        """.trimIndent()

        val slidinRichstyle = """
        {
            "id": "5dd86f44511946ea55132f29",
            "name": "Test serving in-app message",
            "message_type": "slide_in",
            "frequency": "unknown",
            "payload": {
                "title": "Book a tour for Antelope Canyon",
                "body_text": "This is an example of your in-app personalization body text.",
                "image_url": "https://asset-templates.exponea.dev/misc/media/canyon/canyon.jpg",
                "title_text_color": "#000000",
                "title_text_size": "22px",
                "body_text_color": "#000000",
                "body_text_size": "14px",
                "background_color": "#ffffff",
                "container_margin": "0px",
                "container_padding": "0px",
                "container_corner_radius": "8px",
                "overlay_color": "rgba(0, 0, 0, 0.6)",
                "buttons_align": "center",
                "text_position": "bottom",
                "image_enabled": true,
                "image_size": "auto",
                "image_margin": "0px",
                "image_corner_radius": "0px",
                "image_aspect_ratio_width": "4",
                "image_aspect_ratio_height": "3",
                "image_object_fit": "cover",
                "image_overlay_enabled": false,
                "title_enabled": true,
                "title_format": ["bold"],
                "title_align": "center",
                "title_line_height": "24px",
                "title_padding": "12px 0px 0px 0px",
                "title_font_url": "",
                "body_enabled": true,
                "body_format": [],
                "body_align": "center",
                "body_line_height": "16px",
                "body_padding": "0px 24px",
                "body_font_url": "",
                "close_button_enabled": true,
                "close_button_margin": "8px 8px 0px 0px",
                "close_button_image_url": "https://www.svgrepo.com/show/503004/close.svg",
                "close_button_background_color": "rgba(250, 250, 250, 0.6)",
                "close_button_color": "#000000",
                "buttons": [
                    {
                        "button_text": "Action",
                        "button_type": "deep-link",
                        "button_link": "https://bloomreach.com",
                        "button_text_color": "#ffffff",
                        "button_background_color": "#019ACE",
                        "button_width": "hug_text",
                        "button_corner_radius": "4px",
                        "button_margin": "16px 0px 0px 0px",
                        "button_has_border": false,
                        "button_font_size": "14px",
                        "button_line_height": "14px",
                        "button_padding": "16px 36px",
                        "button_align": "center",
                        "button_border_color": "#006081",
                        "button_border_width": "2px",
                        "button_font_url": "",
                        "button_enabled": true
                    },
                    {
                        "button_text": "Action",
                        "button_type": "deep-link",
                        "button_link": "https://bloomreach.com",
                        "button_text_color": "#ffffff",
                        "button_background_color": "#019ACE",
                        "button_width": "hug_text",
                        "button_corner_radius": "4px",
                        "button_margin": "16px 0px 0px 0px",
                        "button_has_border": false,
                        "button_font_size": "14px",
                        "button_line_height": "14px",
                        "button_padding": "16px 36px",
                        "button_align": "center",
                        "button_border_color": "#006081",
                        "button_border_width": "2px",
                        "button_font_url": "",
                        "button_enabled": false
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
                "enabled": false
            },
            "load_delay": 2000,
            "close_timeout": 1000,
            "is_html": false
        }
        """.trimIndent()

        fun buildInAppMessageWithoutRichstyle(
            id: String? = null,
            dateFilter: DateFilter? = null,
            trigger: EventFilter? = null,
            frequency: String? = null,
            imageUrl: String? = null,
            priority: Int? = null,
            timeout: Long? = null,
            delay: Long? = null,
            environment: MockWebServer? = null,
            type: InAppMessageType = MODAL
        ): InAppMessage {
            var payload: InAppMessagePayload? = null
            var payloadHtml: String? = null
            if (type == FREEFORM) {
                payloadHtml = "<html>" +
                    "<head>" +
                    "<style>" +
                    ".css-image {" +
                    "   background-image: url('https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg')" +
                    "}" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<img src='https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg'/>" +
                    "<div data-actiontype='close'>Close</div>" +
                    "<div data-link='https://someaddress.com'>Action 1</div>" +
                    "</body></html>"
            } else {
                payload = InAppMessagePayload(
                    imageUrl = testUrl(imageUrl ?: "https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg", environment),
                    title = "filip.vozar@exponea.com",
                    titleTextColor = "#000000",
                    titleTextSize = "22px",
                    bodyText = "This is an example of your in-app message body text.",
                    bodyTextColor = "#000000",
                    bodyTextSize = "14px",
                    backgroundColor = "#ffffff",
                    closeButtonIconColor = "#ffffff",
                    buttons = arrayListOf(
                        InAppMessagePayloadButton(
                            rawType = "deep-link",
                            text = "Action",
                            link = testUrl("https://someaddress.com", environment),
                            textColor = "#ffffff",
                            backgroundColor = "#f44cac"
                        ),
                        InAppMessagePayloadButton(
                            rawType = "cancel",
                            text = "Cancel",
                            link = null,
                            textColor = "#ffffff",
                            backgroundColor = "#f44cac"
                        )
                    )
                )
            }
            return InAppMessage(
                id = id ?: "5dd86f44511946ea55132f29",
                name = "Test serving in-app message",
                rawMessageType = type.value,
                rawFrequency = frequency ?: "unknown",
                variantId = 0,
                variantName = "Variant A",
                trigger = trigger ?: EventFilter("session_start", emptyList()),
                dateFilter = dateFilter ?: DateFilter(false, null, null),
                priority = priority,
                delay = delay,
                timeout = timeout,
                payload = payload,
                payloadHtml = payloadHtml,
                isHtml = type == FREEFORM,
                consentCategoryTracking = null,
                rawHasTrackingConsent = null,
                isRichText = false
            )
        }

        fun buildInAppMessageWithRichstyle(
            id: String? = null,
            dateFilter: DateFilter? = null,
            trigger: EventFilter? = null,
            frequency: String? = null,
            imageUrl: String? = null,
            priority: Int? = null,
            timeout: Long? = null,
            delay: Long? = null,
            environment: MockWebServer? = null,
            type: InAppMessageType = MODAL,
            imageSizing: String? = "auto",
            textPosition: String? = null,
            isTextOverImage: Boolean? = null,
            isCloseButtonEnabled: Boolean? = true,
            isImageEnabled: Boolean? = true,
            isTitleEnabled: Boolean? = true,
            isBodyEnabled: Boolean? = true,
            titleFontUrl: String? = "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
            bodyFontUrl: String? = "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
            buttonFontUrl: String? = "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf"
        ): InAppMessage {
            var payload: InAppMessagePayload? = null
            var payloadHtml: String? = null
            if (type == FREEFORM) {
                payloadHtml = "<html>" +
                    "<head>" +
                    "<style>" +
                    ".css-image {" +
                    "   background-image: url('https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg')" +
                    "}" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<img src='https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg'/>" +
                    "<div data-actiontype='close'>Close</div>" +
                    "<div data-link='https://someaddress.com'>Action 1</div>" +
                    "</body></html>"
            } else {
                payload = InAppMessagePayload(
                    imageUrl = testUrl(imageUrl ?: "https://i.ytimg.com/vi/t4nM1FoUqYs/maxresdefault.jpg", environment),
                    title = "filip.vozar@exponea.com",
                    titleTextColor = "#000000",
                    titleTextSize = "22px",
                    bodyText = "This is an example of your in-app message body text.",
                    bodyTextColor = "#000000",
                    bodyTextSize = "14px",
                    buttons = arrayListOf(
                        InAppMessagePayloadButton(
                            rawType = "deep-link",
                            text = "Action",
                            link = testUrl("https://someaddress.com", environment),
                            backgroundColor = "blue",
                            textColor = "#ffffff",
                            fontUrl = buttonFontUrl,
                            sizing = "hug",
                            radius = "12dp",
                            margin = "20px 10px 15px 10px",
                            textSize = "24px",
                            lineHeight = "32px",
                            padding = "20px 10px 15px 10px",
                            textStyle = listOf("bold"),
                            borderColor = "black",
                            borderWeight = "1px",
                            isEnabled = true
                        ),
                        InAppMessagePayloadButton(
                            rawType = "cancel",
                            text = "Cancel",
                            link = null,
                            backgroundColor = "#f44cac",
                            textColor = "#ffffff",
                            fontUrl = buttonFontUrl,
                            sizing = "hug",
                            radius = "12dp",
                            margin = "20px 10px 15px 10px",
                            textSize = "24px",
                            lineHeight = "32px",
                            padding = "20px 10px 15px 10px",
                            textStyle = listOf("bold"),
                            borderColor = "black",
                            borderWeight = "1px",
                            isEnabled = true
                        )
                    ),
                    backgroundColor = "#ffffff",
                    closeButtonIconColor = "#ffffff",
                    imageSizing = imageSizing,
                    imageScale = "fill",
                    imageMargin = "200 10 10 10",
                    titleFontUrl = titleFontUrl,
                    titleTextAlignment = "center",
                    titleTextStyle = listOf("bold"),
                    titleLineHeight = "32px",
                    titlePadding = "200px 10px 15px 10px",
                    bodyFontUrl = bodyFontUrl,
                    bodyTextAlignment = "center",
                    bodyTextStyle = listOf("bold"),
                    bodyLineHeight = "32px",
                    bodyPadding = "200px 10px 15px 10px",
                    buttonsAlignment = "center",
                    imageRatioWidth = "16",
                    imageRatioHeight = "9",
                    closeButtonBackgroundColor = "yellow",
                    closeButtonIconUrl = null,
                    closeButtonMargin = "50px 10px",
                    isCloseButtonEnabled = isCloseButtonEnabled,
                    backgroundOverlayColor = "#FF00FF10",
                    textPosition = textPosition,
                    isTextOverImage = isTextOverImage,
                    imageRadius = "10px",
                    isTitleEnabled = isTitleEnabled,
                    isImageEnabled = isImageEnabled,
                    isBodyEnabled = isBodyEnabled
                )
            }
            return InAppMessage(
                id = id ?: "5dd86f44511946ea55132f29",
                name = "Test serving in-app message",
                rawMessageType = type.value,
                rawFrequency = frequency ?: "unknown",
                variantId = 0,
                variantName = "Variant A",
                trigger = trigger ?: EventFilter("session_start", emptyList()),
                dateFilter = dateFilter ?: DateFilter(false, null, null),
                priority = priority,
                delay = delay,
                timeout = timeout,
                payload = payload,
                payloadHtml = payloadHtml,
                isHtml = type == FREEFORM,
                consentCategoryTracking = null,
                rawHasTrackingConsent = null,
                isRichText = true
            )
        }

        private fun testUrl(url: String, environment: MockWebServer?): String {
            if (environment == null) {
                return url
            }
            return environment.url(url).toString()
        }

        fun getInAppMessageForControlGroup(
            id: String? = null,
            dateFilter: DateFilter? = null,
            trigger: EventFilter? = null,
            frequency: String? = null,
            variantId: Int,
            variantName: String,
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
                variantName = variantName,
                trigger = trigger ?: EventFilter("session_start", emptyList()),
                dateFilter = dateFilter ?: DateFilter(false, null, null),
                priority = priority,
                delay = delay,
                timeout = timeout,
                payload = null,
                payloadHtml = null,
                isHtml = false,
                consentCategoryTracking = null,
                rawHasTrackingConsent = null,
                isRichText = false
            )
        }
    }

    @Test
    fun `should parse in-app message from rich-styled json`() {
        assertEquals(
            buildInAppMessageWithRichstyle(timeout = 1000, delay = 2000),
            Gson().fromJson(RICH_STYLE_JSON, InAppMessage::class.java)
        )
    }

    @Test
    fun `should parse in-app message from nonrich-styled json`() {
        assertEquals(
            buildInAppMessageWithoutRichstyle(timeout = 1000, delay = 2000),
            Gson().fromJson(nonRichStyleJson, InAppMessage::class.java)
        )
    }

    @Test
    fun `should serialize and deserialize basic richstyled json`() {
        val deserialized = ExponeaGson.instance.fromJson(RICH_STYLE_JSON, InAppMessage::class.java)
        val serialized = ExponeaGson.instance.toJson(deserialized)
        assertEqualsJsons(RICH_STYLE_JSON, serialized)
    }

    @Test
    fun `should serialize and deserialize basic non-richstyled json`() {
        val deserialized = ExponeaGson.instance.fromJson(nonRichStyleJson, InAppMessage::class.java)
        val serialized = ExponeaGson.instance.toJson(deserialized)
        assertEqualsJsons(nonRichStyleJson, serialized)
    }

    @Test
    fun `should serialize and deserialize fullscreen json`() {
        val deserialized = ExponeaGson.instance.fromJson(fullscreenRichstyle, InAppMessage::class.java)
        val serialized = ExponeaGson.instance.toJson(deserialized)
        assertEqualsJsons(fullscreenRichstyle, serialized)
    }

    @Test
    fun `should serialize and deserialize modal json`() {
        val deserialized = ExponeaGson.instance.fromJson(modalRichstyle, InAppMessage::class.java)
        val serialized = ExponeaGson.instance.toJson(deserialized)
        assertEqualsJsons(modalRichstyle, serialized)
    }

    @Test
    fun `should serialize and deserialize slide-in json`() {
        val deserialized = ExponeaGson.instance.fromJson(slidinRichstyle, InAppMessage::class.java)
        val serialized = ExponeaGson.instance.toJson(deserialized)
        assertEqualsJsons(slidinRichstyle, serialized)
    }
}
