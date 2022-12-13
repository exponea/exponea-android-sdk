package com.exponea.sdk.models

import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.currentTimeSeconds
import com.google.gson.annotations.SerializedName

data class MessageItem(
    @SerializedName("id")
    val id: String,
    val type: String,
    @SerializedName("is_read")
    var read: Boolean? = false,
    @SerializedName("content")
    val rawContent: Map<String, Any?>?
) {
    val content: MessageItemContent?
        get() {
            return when (type) {
                "push" -> parseFromPushNotification(rawContent)
                else -> {
                    Logger.e(this, "AppInbox message has unsupported type \"${type}\"")
                    return null
                }
            }
        }

    val receivedTime: Double get() = content?.createdAt ?: currentTimeSeconds()

    private fun parseFromPushNotification(source: Map<String, Any?>?): MessageItemContent {
        var normalized: Map<String, String> = mapOf()
        if (source != null) {
            normalized = source
                .filter { e -> e.value != null }
                .map { e -> e.key to toJson(e.value!!) }
                .toMap()
        }
        val pushContent = NotificationPayload(HashMap(normalized))
        return MessageItemContent(pushContent)
    }

    private fun toJson(value: Any): String {
        if (value is String) {
            return value
        }
        return ExponeaGson.instance.toJson(value)
    }

    val hasTrackingConsent: Boolean get() {
        return content?.hasTrackingConsent ?: true
    }
}
