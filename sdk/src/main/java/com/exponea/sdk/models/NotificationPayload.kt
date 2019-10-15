package com.exponea.sdk.models

import com.exponea.sdk.util.fromJson
import com.google.gson.Gson
import org.json.JSONArray
import java.util.*

@Suppress("ArrayInDataClass")
data class NotificationPayload(
        val image: String? = null,
        val sound: String? = null,
        val buttons: ArrayList<ActionPayload>? = null,
        val notificationAction: ActionPayload? = null,
        val attributes: Map<String, String>? = null
) {

    /**
     * Construct the payload using the json data
     */
    constructor(map: Map<String, String>) : this(
            map["image"],
            map["sound"],
            handleButtons(map["actions"]),
            handleNotificationAction(map["action"], map["url"]),
            handleAttributes(map["attributes"])
    )

    data class ActionPayload(
            val action: Actions? = null,
            val url: String? = null,
            val title: String? = null
    )

    companion object {

        /**
         * Handle the extra attributes that can be send in the notification
         */
        private fun handleAttributes(attributeJson: String?): Map<String, String>? {
            return if (attributeJson == null) null else Gson().fromJson(attributeJson)
        }

        /**
         * Create an array with all the buttons action payloads
         */
        private fun handleButtons(buttonsJson: String?): ArrayList<ActionPayload>? {
            if (buttonsJson == null)
                return null

            val buttonsArray: ArrayList<ActionPayload> = arrayListOf()
            val buttonsJsonArray = JSONArray(buttonsJson)

            //if we have a button payload, verify each button action
            for (i in 0 until buttonsJsonArray.length()) {
                val item: Map<String, String> = Gson().fromJson(buttonsJsonArray[i].toString())
                val actionEnum = Actions.find(item["action"])
                buttonsArray.add(ActionPayload(actionEnum, item["url"], item["title"]))
            }

            return buttonsArray
        }

        private fun handleNotificationAction(actionString: String?, actionUrl: String?): ActionPayload {
            return ActionPayload(Actions.find(actionString), actionUrl)
        }
    }

    /**
     * Each action is used to trigger a different event when clicking a notification body or button
     */
    enum class Actions(val value: String) {
        APP("app"),
        BROWSER("browser"),
        DEEPLINK("deeplink");

        companion object {
            fun find(value: String?) = Actions.values().find { it.value == value }
        }
    }

}

