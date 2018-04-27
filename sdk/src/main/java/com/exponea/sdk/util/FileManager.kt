package com.exponea.sdk.util

import com.exponea.sdk.models.ExponeaConfiguration
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File

object FileManager {

    val gson = Gson()

    fun getJsonOfFile(filename: String): String? {
        try {
            val filePath = ClassLoader.getSystemResource(filename).file
            val bufferedReader: BufferedReader = File(filePath).bufferedReader()
            val inputString = bufferedReader.use { it.readText() }
            Logger.d(this,"Configuration file successfully loaded")
            return inputString
        } catch (e:Exception){
            Logger.e(this,"Could not load configuration file $filename")
            return null
        }
    }

    fun getConfigurationOfFile(filename: String): ExponeaConfiguration {
        val data = getJsonOfFile(filename)

        if (data == null) {
            Logger.e(this, "No data found on Configuration file $filename")
        }

        val configuration = gson.fromJson(data, ExponeaConfiguration::class.java)

        if (configuration == null) {
            Logger.e(this, "Could not parse XML from file $filename to ExponeaConfiguration")
            return ExponeaConfiguration()
        }

        return configuration
    }
}