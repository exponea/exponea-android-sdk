package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.Constants
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.Logger
import com.google.gson.Gson

internal object ConfigurationFileManager {
    fun getConfigurationFromDefaultFile(context: Context): ExponeaConfiguration? {
        val data = readContentFromDefaultFile(context)

        if (data.isNullOrEmpty()) {
            Logger.e(this, "No data found on Configuration file")
            return null
        }
        val configuration = Gson().fromJson(data, ExponeaConfiguration::class.java)
        // Gson uses reflection for deserialization, so we manually call setters to apply custom logic in deprecated property setters
        configuration.authorization?.let { configuration.authorization = it }
        configuration.baseURL.takeIf { it != Constants.Repository.baseURL }?.let { configuration.baseURL = it }
        configuration.projectToken.takeIf { it.isNotEmpty() }?.let { configuration.projectToken = it }
        configuration.projectRouteMap.takeIf { it.isNotEmpty() }?.let { configuration.projectRouteMap = it }

        return configuration
    }

    private fun readContentFromDefaultFile(context: Context): String? {
        return try {
            val configurationFileName = "exponea_configuration.json"
            val inputStream =
                javaClass.classLoader?.getResourceAsStream(configurationFileName)
                    ?: context.assets.open(configurationFileName)
            val buffer = inputStream.bufferedReader()
            val inputString = buffer.use { it.readText() }
            Logger.d(this, "Configuration file successfully loaded")
            inputString
        } catch (_: Exception) {
            Logger.e(this, "Could not load configuration file ")
            null
        }
    }
}
