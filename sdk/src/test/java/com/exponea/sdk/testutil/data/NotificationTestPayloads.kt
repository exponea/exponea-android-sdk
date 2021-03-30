package com.exponea.sdk.testutil.data

internal object NotificationTestPayloads {
    /* ktlint-disable max-line-length */
    val BASIC_NOTIFICATION = hashMapOf(
        "action" to "app",
        "title" to "push title",
        "message" to "push notification message"
    )
    val DEEPLINK_NOTIFICATION = hashMapOf(
        "action" to "deeplink",
        "url" to "app://test",
        "title" to "push title",
        "message" to "push notification message"
    )
    val BROWSER_NOTIFICATION = hashMapOf(
        "action" to "browser",
        "url" to "http://google.com",
        "title" to "push title",
        "message" to "push notification message"
    )

    val ACTIONS_NOTIFICATION = hashMapOf(
        "action" to "app",
        "actions" to "[{\"action\":\"app\",\"title\":\"Action 1 title\"},{\"action\":\"deeplink\",\"title\":\"Action 2 title\",\"url\":\"app:\\/\\/deeplink\"},{\"action\":\"browser\",\"title\":\"Action 3 title\",\"url\":\"http:\\/\\/google.com\"}]",
        "title" to "push title",
        "message" to "push notification message"
    )

    val PRODUCTION_NOTIFICATION = hashMapOf(
        "notification_id" to "1",
        "action" to "app",
        "actions" to """[
            {"action":"app","title":"Action 1 title"},
            {"action":"deeplink","title":"Action 2 title","url":"http:\/\/deeplink?search=something"},
            {"action":"browser","title":"Action 3 title","url":"http:\/\/google.com?search=something"}
        ]""",
        "url_params" to """{"utm_campaign":"Testing mobile push","utm_medium":"mobile_push_notification","utm_source":"exponea"}""",
        "title" to "Notification title",
        "attributes" to """{
            "campaign_name":"Wassil's push",
            "event_type":"campaign",
            "action_id":2,
            "action_type":"mobile notification",
            "campaign_policy":"",
            "subject":"Notification title",
            "action_name":"Unnamed mobile push",
            "recipient":"eMxrdLuMalE:APA91bFgzKPVtem5aA0ZL0PFm_FgksAtVCOhzIQywX7DZQx2dKiVUepgl_Yw2aIrGZ7gpblCHltL6PWfXLoRw_5aZvV9swkPtUNwYjMNoF2f7igXgNe5Ovgyi8q5fmoX9QVHtyt8C-0Z",
            "some property":"some value",
            "language":"",
            "campaign_id":"5db9ab54b073dfb424ccfa6f",
            "platform":"android",
            "sent_timestamp":1614585422.20,
            "type":"push"
        }""",
        "message" to "Notification text"
    )

    val SILENT_NOTIFICATION = hashMapOf(
        "title" to "Silent push",
        "action" to "app",
        "silent" to "true",
        "attributes" to """{ "silent_test": "value" }"""
    )
    /* ktlint-enable== max-line-length */
}
