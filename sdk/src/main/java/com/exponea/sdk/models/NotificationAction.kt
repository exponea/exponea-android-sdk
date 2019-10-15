package com.exponea.sdk.models

import java.io.Serializable


data class NotificationAction(
        val actionType: String,
        val actionName: String? = null,
        val url: String? = null
) : Serializable {

    fun toHashMap(): HashMap<String, Any> {
        return hashMapOf(
                "notification_action_type" to actionType,
                "notification_action_name" to (actionName ?: ""),
                "notification_action_url" to (url ?: ""),
                "os_name" to "Android"

        )
    }

    companion object {
        const val ACTION_TYPE_BUTTON = "button"
        const val ACTION_TYPE_NOTIFICATION = "notification"
    }
}
