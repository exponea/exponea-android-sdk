package com.exponea.sdk.repository

import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.util.fromJson
import com.google.gson.Gson

internal class PushNotificationRepositoryImpl(
    private val preferences: ExponeaPreferences
) : PushNotificationRepository {

    private val KEY_EXTRA_DATA = "ExponeaPushNotificationExtraData"

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

    override fun clearExtraData() {
        preferences.remove(KEY_EXTRA_DATA)
    }
}
