package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.util.Logger
import com.google.gson.Gson

internal object ExponeaConfigRepository {

    internal const val PREF_CONFIG = "ExponeaConfigurationPref"

    fun set(context: Context, configuration: ExponeaConfiguration) {
        if (Exponea.isStopped) {
            Logger.e(this, "Last known SDK configuration store failed, SDK is stopping")
            return
        }
        val prefs = ExponeaPreferencesImpl(context)
        val gson = Gson()
        val jsonConfiguration = gson.toJson(configuration)
        prefs.setString(PREF_CONFIG, jsonConfiguration)
    }

    fun get(context: Context): ExponeaConfiguration? {
        if (Exponea.isStopped) {
            Logger.e(this, "Last known SDK configuration load failed, SDK is stopping")
            return null
        }
        val prefs = ExponeaPreferencesImpl(context)
        val gson = Gson()
        val jsonConfig = prefs.getString(PREF_CONFIG, "")
        if (jsonConfig.isEmpty())
            return null

        return try {
            gson.fromJson(jsonConfig, ExponeaConfiguration::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        ExponeaPreferencesImpl(context).remove(PREF_CONFIG)
    }
}
