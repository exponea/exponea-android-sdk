package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import java.io.File

internal class FileManagerImpl : FileManager {

    val gson = Gson()

    override fun createFile(filename: String, type: String) {
        createTempFile(
                prefix = filename,
                suffix = type
        )
    }

    override fun writeToFile(filename: String, text: String) {
        val filePath = ClassLoader.getSystemResource(filename).file
        val bufferedWriter = File(filePath).bufferedWriter()
        bufferedWriter.write(text)
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

    override fun getConfigurationFromDefaultFile(context: Context): ExponeaConfiguration? {
        val data = readContentFromDefaultFile(context)

        if (data.isNullOrEmpty()) {
            Logger.e(this, "No data found on Configuration file")
            return null
        }

        return gson.fromJson(data, ExponeaConfiguration::class.java)
    }
}
