package com.exponea.example.managers

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.provider.Settings

@SuppressLint("RegisteredID")
class RegisteredIdManager (private val context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    init {
        val dId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        prefs.edit().putString(PREF_REGID, dId).apply()
    }

    companion object {
        const val PREF_REGID = "RegID"
    }

    var registeredID: String = prefs.getString(PREF_REGID, "")
        get() = prefs.getString(PREF_REGID, "")
        set(value) {
            prefs.edit().putString(PREF_REGID, value).apply()
            field = value
        }
}