package com.exponea.example.managers

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.provider.Settings

@SuppressLint("HardwareIds")
class UserIdManager(private val context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    init {
        val dId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        prefs.edit().putString(PREF_UID, dId).apply()
    }

    companion object {
        const val PREF_UID = "UserUID"
    }

    var uniqueUserID: String = prefs.getString(PREF_UID, "")
    get() = prefs.getString(PREF_UID, "")
    set(value) {
        prefs.edit().putString(PREF_UID, value).apply()
        field = value
    }

}