package com.exponea.sdk.util

import com.exponea.sdk.Exponea
import com.exponea.sdk.models.CampaignData
import com.exponea.sdk.models.ExponeaNotificationActionType.APP
import com.exponea.sdk.models.ExponeaNotificationActionType.BROWSER
import com.exponea.sdk.models.ExponeaNotificationActionType.DEEPLINK
import com.exponea.sdk.models.MessageItemAction
import com.exponea.sdk.models.MessageItemAction.Type
import com.exponea.sdk.models.MessageItemContent
import com.exponea.sdk.models.NotificationPayload
import com.exponea.sdk.models.NotificationPayload.ActionPayload
import com.exponea.sdk.util.HtmlNormalizer.ActionInfo
import com.exponea.sdk.util.HtmlNormalizer.HtmlNormalizerConfig
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder

/**
 * Defines parsing of MessageItemContent from MessageItem data
 */
internal class AppInboxParser {
    companion object {

        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

        fun parseFromHtmlMessage(source: Map<String, Any?>?): MessageItemContent {
            val normalized = normalizeData(source)
            val actions: MutableList<MessageItemAction> = mutableListOf()
            val htmlOrigin = normalized["message"]
            if (htmlOrigin != null) {
                val imageCache = Exponea.getComponent()?.appInboxMessagesBitmapCache
                val fontCache = Exponea.getComponent()?.fontCache
                if (imageCache == null || fontCache == null) {
                    throw Exception("Exponea SDK was not initialized properly!")
                }
                val htmlContent = HtmlNormalizer(
                    imageCache,
                    fontCache,
                    htmlOrigin
                ).normalize(config = HtmlNormalizerConfig(
                    makeResourcesOffline = false,
                    ensureCloseButton = false
                ))
                htmlContent.actions?.forEach { htmlAction ->
                    actions.add(toAppInboxAction(htmlAction))
                }
            }
            val trackingData: MutableMap<String, Any?> = mutableMapOf()
            gson.fromJson<Map<String, Any?>>(normalized["attributes"] ?: "{}")?.let {
                trackingData.putAll(it)
            }
            trackingData.remove("event_type")
            gson.fromJson<Map<String, String>>(normalized["url_params"] ?: "{}")?.let {
                trackingData.putAll(CampaignData(it).getTrackingData())
            }
            return MessageItemContent(
                imageUrl = normalized["image"],
                title = normalized["title"],
                message = normalized["pre_header"],
                consentCategoryTracking = normalized["consent_category_tracking"],
                hasTrackingConsent = GdprTracking.hasTrackingConsent(normalized["has_tracking_consent"]),
                trackingData = trackingData,
                actions = actions,
                html = htmlOrigin
            )
        }

        fun parseFromPushNotification(source: Map<String, Any?>?): MessageItemContent {
            val normalized = normalizeData(source)
            val notificationPayload = NotificationPayload(HashMap(normalized))
            return MessageItemContent(
                imageUrl = notificationPayload.image,
                title = notificationPayload.title,
                message = notificationPayload.message,
                consentCategoryTracking = notificationPayload.notificationData.consentCategoryTracking,
                hasTrackingConsent = notificationPayload.notificationData.hasTrackingConsent,
                trackingData = notificationPayload.notificationData.getTrackingData(),
                actions = notificationPayload.buttons?.map {
                    notifAction -> toAppInboxAction(notifAction)
                } ?: emptyList(),
                action = notificationPayload.notificationAction.let { toAppInboxAction(it) }
            )
        }

        private fun normalizeData(source: Map<String, Any?>?): Map<String, String> {
            var normalized: Map<String, String> = mapOf()
            if (source != null) {
                normalized = source
                    .filter { e -> e.value != null }
                    .map { e -> e.key to toJson(e.value!!) }
                    .toMap()
            }
            return normalized
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

        private fun toAppInboxAction(source: ActionInfo): MessageItemAction {
            var target = MessageItemAction()
            target.title = source.buttonText
            target.url = source.actionUrl
            if (source.actionUrl.startsWith("http://") ||
                source.actionUrl.startsWith("https://")) {
                target.type = Type.BROWSER
            } else {
                target.type = Type.DEEPLINK
            }
            return target
        }

        private fun toJson(value: Any): String {
            if (value is String) {
                return value
            }
            return ExponeaGson.instance.toJson(value)
        }
    }
}
