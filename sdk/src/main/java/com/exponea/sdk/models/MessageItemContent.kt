package com.exponea.sdk.models

import com.exponea.sdk.models.MessageItemAction.Type
import com.exponea.sdk.models.NotificationPayload.ActionPayload
import com.exponea.sdk.models.NotificationPayload.Actions.APP
import com.exponea.sdk.models.NotificationPayload.Actions.BROWSER
import com.exponea.sdk.models.NotificationPayload.Actions.DEEPLINK
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds

class MessageItemContent {

    var imageUrl: String? = null
    var title: String? = null
    var message: String? = null
    var createdAt: Double? = currentTimeSeconds()
    var consentCategoryTracking: String? = null
    var hasTrackingConsent: Boolean = true
    var trackingData: Map<String, Any?> = emptyMap()
    var actions: List<MessageItemAction> = emptyList()
    var action: MessageItemAction? = null

    internal constructor(source: NotificationPayload) {
        imageUrl = source.image
        title = source.title
        message = source.message
        createdAt = source.deliveredTimestamp
        consentCategoryTracking = source.notificationData.consentCategoryTracking
        hasTrackingConsent = source.notificationData.hasTrackingConsent
        trackingData = source.notificationData.getTrackingData()
        actions = source.buttons?.map { notifAction -> toAppInboxAction(notifAction) } ?: emptyList()
        action = source.notificationAction.let { toAppInboxAction(it) }
    }

    private fun toAppInboxAction(source: ActionPayload): MessageItemAction {
        val target = MessageItemAction()
        target.title = source.title
        target.url = source.url
        target.type = when (source.action) {
            APP -> Type.APP
            BROWSER -> Type.BROWSER
            DEEPLINK -> Type.DEEPLINK
            else -> {
                Logger.e(this, "Unsupported PushNotif action \"${source.action}\"")
                Type.NO_ACTION
            }
        }
        return target
    }
}
