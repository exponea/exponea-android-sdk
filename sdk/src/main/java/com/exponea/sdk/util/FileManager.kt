package com.exponea.sdk.util

import com.exponea.sdk.models.ExponeaConfiguration
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File

object FileManager {
    val gson = Gson()

    private fun getJsonOfFile(filename: String): String? {
        return try {
            val filePath = ClassLoader.getSystemResource(filename).file
            val bufferedReader: BufferedReader = File(filePath).bufferedReader()
            val inputString = bufferedReader.use { it.readText() }
            Logger.d(this, "Configuration file successfully loaded")
            inputString
        } catch (e: Exception) {
            Logger.e(this, "Could not load configuration file $filename")
            null
        }
    }

    fun getConfigurationOfFile(filename: String): ExponeaConfiguration? {
        val data = getJsonOfFile(filename)

        if (data.isNullOrEmpty()) {
            Logger.e(this, "No data found on Configuration file $filename")
            return null
        }

        return gson.fromJson(data, ExponeaConfiguration::class.java)
    }
}