package com.exponea.sdk.models

import com.exponea.sdk.util.GdprTracking
import com.exponea.sdk.util.fromJson
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import org.json.JSONArray

internal class NotificationPayload(val rawData: HashMap<String, String>) {
    val notificationId: Int = rawData["notification_id"]?.toInt() ?: 0
    val silent: Boolean = rawData["silent"] == "true"
    val title: String = rawData["title"] ?: ""
    val message: String = rawData["message"] ?: ""
    val image: String? = rawData["image"]
    val sound: String? = rawData["sound"]
    val buttons: ArrayList<ActionPayload>? = parseActions(rawData["actions"])
    val notificationAction: ActionPayload = parseMainAction(rawData["action"], rawData["url"])
    val notificationData: NotificationData = parseNotificationData(rawData)
    val attributes: Map<String, Any>? = parseAttributes(rawData["attributes"])
    var deliveredTimestamp: Double? = notificationData.sentTimestamp

    data class ActionPayload(
        val action: Actions? = null,
        val url: String? = null,
        val title: String? = null
    )

    companion object {
        val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()
        /**
         * Parse notification data to use for tracking purposes
         */
        private fun parseNotificationData(data: Map<String, String>): NotificationData {
            val dataMap: HashMap<String, Any> = gson.fromJson(data["data"] ?: data["attributes"] ?: "{}")
            val campaignMap: Map<String, String> = gson.fromJson(data["url_params"] ?: "{}")
            val consentCategoryTracking: String? = data["consent_category_tracking"]
            val hasTrackingConsent: Boolean = GdprTracking.hasTrackingConsent(data["has_tracking_consent"])
            return NotificationData(
                dataMap,
                campaignMap,
                consentCategoryTracking,
                hasTrackingConsent
            )
        }

        /**
         * Parse the extra attributes that can be send in the notification
         */
        private fun parseAttributes(attributeJson: String?): Map<String, Any>? {
            return if (attributeJson == null) null else gson.fromJson(attributeJson)
        }

        /**
         * Create an array with all the buttons action payloads
         */
        private fun parseActions(buttonsJson: String?): ArrayList<ActionPayload>? {
            if (buttonsJson == null)
                return null

            val buttonsArray: ArrayList<ActionPayload> = arrayListOf()
            val buttonsJsonArray = JSONArray(buttonsJson)

            // if we have a button payload, verify each button action
            for (i in 0 until buttonsJsonArray.length()) {
                val item: Map<String, String> = gson.fromJson(buttonsJsonArray[i].toString())
                val actionEnum = Actions.find(item["action"])
                buttonsArray.add(ActionPayload(actionEnum, item["url"], item["title"]))
            }

            return buttonsArray
        }

        private fun parseMainAction(actionString: String?, actionUrl: String?): ActionPayload {
            return ActionPayload(Actions.find(actionString), actionUrl)
        }
    }

    /**
     * Each action is used to trigger a different event when clicking a notification body or button
     */
    enum class Actions(val value: String) {
        APP("app"),
        BROWSER("browser"),
        DEEPLINK("deeplink"),
        SELFCHECK("self-check");

        companion object {
            fun find(value: String?) = Actions.values().find { it.value == value }
        }
    }
}
