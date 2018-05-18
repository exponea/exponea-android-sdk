package com.exponea.sdk.manager

import com.exponea.sdk.models.ExponeaConfiguration

interface FileManager {
    fun createFile(filename: String, type: String)
    fun writeToFile(filename: String, text: String)
    fun readContentFromFile(filename: String): String?
    fun deleteFile(filename: String)
    fun getConfigurationFromFile(filename: String): ExponeaConfiguration?
}