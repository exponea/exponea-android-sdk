package com.exponea.sdk.preferences

import android.content.Context
import android.preference.PreferenceManager

class ExponeaPreferencesImpl(context: Context) : ExponeaPreferences {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun setString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    override fun setLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    override fun getString(key: String, default: String): String {
        return sharedPreferences.getString(key, default)
    }

    override fun getBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    override fun getLong(key: String, default: Long): Long {
        return sharedPreferences.getLong(key, default)
    }

    override fun remove(key: String): Boolean {
        sharedPreferences.edit().remove(key).apply()
        return true
    }
}