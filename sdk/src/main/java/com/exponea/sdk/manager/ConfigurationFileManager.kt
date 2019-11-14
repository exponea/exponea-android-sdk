package com.exponea.sdk.manager

import android.content.Context
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

        return Gson().fromJson(data, ExponeaConfiguration::class.java)
    }

    private fun readContentFromDefaultFile(context: Context): String? {
        return try {
            val configurationFileName = "exponea_configuration.json"
            var inputStream = javaClass.classLoader.getResourceAsStream(configurationFileName)

            if (inputStream == null)
                inputStream = context.assets.open(configurationFileName)

            val buffer = inputStream.bufferedReader()

            val inputString = buffer.use { it.readText() }
            Logger.d(this, "Configuration file successfully loaded")
            inputString
        } catch (e: Exception) {
            Logger.e(this, "Could not load configuration file ")
            null
        }
    }
}