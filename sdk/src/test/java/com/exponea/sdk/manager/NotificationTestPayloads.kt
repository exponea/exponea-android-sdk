package com.exponea.sdk.manager

internal object NotificationTestPayloads {
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

    val ATTRIBUTES_NOTIFICATION = hashMapOf(
        "action" to "app",
        "title" to "push title",
        "attributes" to "{\"campaign_name\":\"push with buttons\",\"action_id\":2,\"something_else\":\"some other value\",\"campaign_id\":\"5db1582b1b2be24d0bee4de9\",\"something\":\"some value\"}",
        "message" to "push notification message"
    )
}