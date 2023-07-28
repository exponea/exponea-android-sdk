package com.exponea.sdk.manager

import com.exponea.sdk.models.InAppContentBlock
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.MessageItem
import com.exponea.sdk.models.NotificationAction
import com.exponea.sdk.models.NotificationData

internal interface TrackingConsentManager {
    fun trackClickedPush(data: NotificationData?, actionData: NotificationAction?, timestamp: Double?, mode: MODE)
    fun trackDeliveredPush(data: NotificationData?, timestamp: Double, mode: MODE)
    fun trackInAppMessageShown(message: InAppMessage, mode: MODE)
    fun trackInAppMessageClick(message: InAppMessage, buttonText: String?, buttonLink: String?, mode: MODE)
    fun trackInAppMessageClose(message: InAppMessage, userInteraction: Boolean, mode: MODE)
    fun trackInAppMessageError(message: InAppMessage, error: String, mode: MODE)
    fun trackAppInboxOpened(item: MessageItem, mode: MODE)
    fun trackAppInboxClicked(message: MessageItem, buttonText: String?, buttonLink: String?, mode: MODE)
    fun trackInAppContentBlockShown(placeholderId: String, contentBlock: InAppContentBlock, mode: MODE)
    fun trackInAppContentBlockClick(
        placeholderId: String,
        contentBlock: InAppContentBlock,
        buttonText: String?,
        buttonLink: String?,
        mode: MODE
    )
    fun trackInAppContentBlockClose(placeholderId: String, contentBlock: InAppContentBlock, mode: MODE)
    fun trackInAppContentBlockError(placeholderId: String, contentBlock: InAppContentBlock, error: String, mode: MODE)

    enum class MODE {
        CONSIDER_CONSENT, IGNORE_CONSENT
    }
}
