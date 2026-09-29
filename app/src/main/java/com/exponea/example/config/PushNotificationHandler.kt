package com.exponea.example.config

import com.exponea.sdk.models.ExponeaNotificationActionType
import com.exponea.sdk.models.PushNotificationDelegate

data class PushNotificationEvent(
    val title: String,
    val message: String
)

object PushNotificationHandler {

    fun create(onEvent: (PushNotificationEvent) -> Unit): PushNotificationDelegate {
        return object : PushNotificationDelegate {
            override fun onSilentPushNotificationReceived(notificationData: Map<String, Any>) {
                informPushNotificationAction("Silent", "received", notificationData, onEvent)
            }

            override fun onPushNotificationReceived(notificationData: Map<String, Any>) {
                informPushNotificationAction("Normal", "received", notificationData, onEvent)
            }

            override fun onPushNotificationOpened(
                action: ExponeaNotificationActionType,
                url: String?,
                notificationData: Map<String, Any>
            ) {
                informPushNotificationAction(action.name, "clicked $url", notificationData, onEvent)
            }
        }
    }

    private fun informPushNotificationAction(
        notifType: String,
        notifFlow: String,
        notificationData: Map<String, Any>,
        onEvent: (PushNotificationEvent) -> Unit
    ) {
        val message = """
            $notifType Push data $notifFlow:
            ${notificationData.entries.joinToString { "${it.key}: ${it.value}" }}
        """.trimIndent()
        onEvent(
            PushNotificationEvent(
                title = "$notifType Push notification $notifFlow",
                message = message
            )
        )
    }
}
