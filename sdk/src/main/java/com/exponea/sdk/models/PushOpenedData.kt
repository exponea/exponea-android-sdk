package com.exponea.sdk.models

data class PushOpenedData(
    val actionType: ExponeaNotificationActionType,
    val actionUrl: String?,
    val extraData: Map<String, String>
)
