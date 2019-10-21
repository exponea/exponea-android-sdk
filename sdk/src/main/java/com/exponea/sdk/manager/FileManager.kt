package com.exponea.sdk.manager

import android.content.Context
import com.exponea.sdk.models.ExponeaConfiguration

interface FileManager {
    fun createFile(filename: String, type: String)
    fun writeToFile(filename: String, text: String)
    fun getConfigurationFromDefaultFile(context: Context): ExponeaConfiguration?
}
