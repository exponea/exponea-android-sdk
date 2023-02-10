package com.exponea.sdk.models

import com.exponea.sdk.models.AppInboxMessateType.HTML
import com.exponea.sdk.models.AppInboxMessateType.PUSH
import com.exponea.sdk.util.AppInboxParser
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.google.gson.annotations.SerializedName

data class MessageItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val rawType: String,
    @SerializedName("is_read")
    var read: Boolean? = false,
    @SerializedName("create_time")
    var receivedTime: Double? = currentTimeSeconds(),
    @SerializedName("content")
    val rawContent: Map<String, Any?>?
) {

    internal var customerIds: Map<String, String?> = mapOf()
    internal var syncToken: String? = null

    val type: AppInboxMessateType get() {
        return when (rawType) {
            "push" -> AppInboxMessateType.PUSH
            "html" -> AppInboxMessateType.HTML
            else -> AppInboxMessateType.UNKNOWN
        }
    }

    val content: MessageItemContent? get() {
        return when (type) {
            PUSH -> AppInboxParser.parseFromPushNotification(rawContent)
            HTML -> AppInboxParser.parseFromHtmlMessage(rawContent ?: mapOf())
            else -> {
                Logger.e(this, "AppInbox message has unsupported type \"${type}\"")
                return null
            }
        }
    }

    val hasTrackingConsent: Boolean get() {
        return content?.hasTrackingConsent ?: true
    }
}

enum class AppInboxMessateType {
    /**
     * AppInbox message with paylod containing PushNotification data
     */
    PUSH,
    /**
     * AppInbox message with paylod containing HTML data
     */
    HTML,
    /**
     * AppInbox message with unknown or invalid type
     */
    UNKNOWN
}
