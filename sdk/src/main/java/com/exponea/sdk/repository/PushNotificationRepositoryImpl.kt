package com.exponea.sdk.repository

import com.exponea.sdk.models.PushOpenedData
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.fromJson
import com.google.gson.Gson

internal class PushNotificationRepositoryImpl(
    private val preferences: ExponeaPreferences
) : PushNotificationRepository {

    companion object {
        const val MAX_STORED_NOTIFICATIONS = 100
        const val KEY_EXTRA_DATA = "ExponeaPushNotificationExtraData"
        const val KEY_DELIVERED_DATA = "ExponeaDeliveredPushNotificationData"
        const val KEY_CLICKED_DATA = "ExponeaClickedPushNotificationData"
    }

    override fun getExtraData(): Map<String, Any>? {
        val dataString = preferences.getString(KEY_EXTRA_DATA, "")
        if (dataString.isEmpty()) {
            return null
        }
        return Gson().fromJson<HashMap<String, Any>>(dataString)
    }

    override fun setExtraData(data: Map<String, Any>) {
        val dataString = Gson().toJson(data)
        preferences.setString(KEY_EXTRA_DATA, dataString)
    }

    override fun appendDeliveredNotification(data: Map<String, String>) {
        val storedDeliveredNotifications = getDeliveredNotifications().let {
            if (it.size >= MAX_STORED_NOTIFICATIONS) {
                it.takeLast(MAX_STORED_NOTIFICATIONS - 1)
            } else {
                it
            }
        }
        val newDeliveredNotifications = storedDeliveredNotifications + data
        val dataString = Gson().toJson(newDeliveredNotifications)
        preferences.setString(KEY_DELIVERED_DATA, dataString)
    }

    override fun popDeliveredPushData(): List<Map<String, Any>> {
        val storedDeliveredNotifications = getDeliveredNotifications()
        clearDeliveredData()
        return storedDeliveredNotifications
    }

    private fun clearDeliveredData() {
        preferences.remove(KEY_DELIVERED_DATA)
    }

    private fun getDeliveredNotifications(): List<Map<String, String>> = runCatching {
        val dataString = preferences.getString(KEY_DELIVERED_DATA, "")
        if (dataString.isEmpty()) {
            return emptyList()
        }
        return Gson().fromJson<List<Map<String, String>>>(dataString)
    }.getOrElse {
        Logger.e(this, "Unable to read delivered notifications stored locally", it)
        return@getOrElse emptyList()
    }

    override fun appendClickedNotification(data: PushOpenedData) {
        val storedClickedNotifications = getClickedNotifications()
        val newClickedNotifications = storedClickedNotifications + data
        val dataString = Gson().toJson(newClickedNotifications)
        preferences.setString(KEY_CLICKED_DATA, dataString)
    }

    override fun popClickedPushData(): List<PushOpenedData> {
        val storedClickedNotifications = getClickedNotifications()
        clearClickedData()
        return storedClickedNotifications
    }

    private fun clearClickedData() {
        preferences.remove(KEY_CLICKED_DATA)
    }

    private fun getClickedNotifications(): List<PushOpenedData> = runCatching {
        val dataString = preferences.getString(KEY_CLICKED_DATA, "")
        if (dataString.isEmpty()) {
            return emptyList()
        }
        return Gson().fromJson<List<PushOpenedData>>(dataString)
    }.getOrElse {
        Logger.e(this, "Unable to read clicked notifications stored locally", it)
        return@getOrElse emptyList()
    }

    override fun clearExtraData() {
        preferences.remove(KEY_EXTRA_DATA)
    }

    override fun clearAll() {
        clearExtraData()
        clearDeliveredData()
        clearClickedData()
    }
}
