package com.exponea.example.managers

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.preference.PreferenceManager

@SuppressLint("RegisteredID")
class RegisteredIdManager(context: Context) {
    private val application = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        const val PREF_REGID = "RegID"
    }

    var registeredID: String? = prefs.getString(PREF_REGID, "")
        get() {
            var id = prefs.getString(PREF_REGID, "")
            if (id == null || id.isEmpty()) {
                id = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
            }
            return id
        }
        set(value) {
            prefs.edit().putString(PREF_REGID, value).apply()
            field = value
        }
}
