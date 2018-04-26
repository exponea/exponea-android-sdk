package com.exponea.sdk.manager

import com.exponea.sdk.models.ExponeaConfiguration
import com.exponea.sdk.util.Logger
import com.google.gson.Gson
import java.io.File

class FileManagerImpl(
        private val gson: Gson
) : FileManager {
    override fun getJsonOfFile(filename: String): String? {
        try {
            val content: String = File(ClassLoader.getSystemResource(filename).file).readText(charset = Charsets.UTF_8)
            Logger.d(this,"Configuration file successfully loaded")
            return content
        } catch (e:Exception){
            Logger.e(this,"Could not load JSON from file $filename")
            return null
        }
    }

    override fun getConfigurationOfFile(filename: String): ExponeaConfiguration {
        val data = getJsonOfFile(filename)

        if (data == null) {
            Logger.e(this, "No data found on Configuration file $filename")
        }

        val configuration = gson.fromJson(data, ExponeaConfiguration::class.java)

        if (configuration == null) {
            Logger.e(this, "Could not parse JSON from file $filename to ExponeaConfiguration")
            return ExponeaConfiguration()
        }

        return configuration
    }
}