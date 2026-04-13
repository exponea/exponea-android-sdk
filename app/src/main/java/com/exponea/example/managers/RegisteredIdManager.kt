package com.exponea.example.managers

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.core.content.edit
import androidx.preference.PreferenceManager

@SuppressLint("RegisteredID")
class RegisteredIdManager(context: Context) {
    private val application = context.applicationContext
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        const val PREF_REGID = "RegID"
    }

    var registeredID: String
        get() = prefs.getString(PREF_REGID, "").takeIf { !it.isNullOrEmpty() }
            ?: Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        set(value) {
            prefs.edit { putString(PREF_REGID, value) }
        }
}
