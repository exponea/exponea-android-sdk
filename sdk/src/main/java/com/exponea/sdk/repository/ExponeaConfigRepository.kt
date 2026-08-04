package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.models.IntegrationConfig
import com.exponea.sdk.models.IntegrationConfigJsonAdapter
import com.exponea.sdk.preferences.ExponeaPreferences
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.util.Logger
import com.google.gson.GsonBuilder

internal object ExponeaConfigRepository {

    internal const val PREF_CONFIG = "ExponeaConfigurationPref"

    fun set(context: Context, configuration: ExponeaConfiguration) {
        if (Exponea.isStopped) {
            Logger.e(this, "Last known SDK configuration store failed, SDK is stopping")
            return
        }

        val gson = GsonBuilder()
            .registerTypeAdapter(IntegrationConfig::class.java, IntegrationConfigJsonAdapter())
            .create()
        val jsonConfiguration = gson.toJson(configuration)

        ExponeaPreferencesImpl(context).setString(PREF_CONFIG, jsonConfiguration)
    }

    fun get(context: Context): ExponeaConfiguration? {
        if (Exponea.isStopped) {
            Logger.e(this, "Last known SDK configuration load failed, SDK is stopping")
            return null
        }

        return get(ExponeaPreferencesImpl(context))
    }

    /**
     * Reads configuration from the given [prefs] instance (same active file as deintegrate cleanup).
     */
    fun get(prefs: ExponeaPreferences): ExponeaConfiguration? {
        return parseConfiguration(prefs.getString(PREF_CONFIG, ""))
    }

    private fun parseConfiguration(jsonConfig: String): ExponeaConfiguration? {
        if (jsonConfig.isEmpty()) {
            return null
        }

        return try {
            val gson = GsonBuilder()
                .registerTypeAdapter(IntegrationConfig::class.java, IntegrationConfigJsonAdapter())
                .create()
            val configuration = gson.fromJson(jsonConfig, ExponeaConfiguration::class.java)
            // Gson uses reflection for deserialization, so we manually call setters to apply custom logic in deprecated property setters
            configuration.authorization?.let { configuration.authorization = it }
            configuration.baseURL.takeIf { it != Constants.Repository.baseURL }?.let { configuration.baseURL = it }
            configuration.projectToken.takeIf { it.isNotEmpty() }?.let { configuration.projectToken = it }
            configuration.projectRouteMap.takeIf { it.isNotEmpty() }?.let { configuration.projectRouteMap = it }

            configuration
        } catch (_: Exception) {
            Logger.e(this, "Failed to parse stored Exponea configuration")
            null
        }
    }
}
