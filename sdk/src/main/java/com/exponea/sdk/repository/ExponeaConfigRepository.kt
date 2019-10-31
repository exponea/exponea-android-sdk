package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.google.gson.Gson

internal object ExponeaConfigRepository {

    private const val PREF_CONFIG = "ExponeaConfigurationPref"

    fun set(context: Context, configuration: ExponeaConfiguration) {
        val prefs = ExponeaPreferencesImpl(context)
        val gson = Gson()
        val jsonConfiguration = gson.toJson(configuration)
        prefs.setString(PREF_CONFIG, jsonConfiguration)
    }

    fun get(context: Context): ExponeaConfiguration? {
        val prefs = ExponeaPreferencesImpl(context)
        val gson = Gson()
        val jsonConfig = prefs.getString(PREF_CONFIG, "")
        if (jsonConfig.isEmpty())
            return null

        return try {
            gson.fromJson<ExponeaConfiguration>(jsonConfig, ExponeaConfiguration::class.java)
        } catch (e: Exception) {
            null
        }
    }

}
