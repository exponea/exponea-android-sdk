package com.exponea.sdk.manager

import com.exponea.sdk.ExponeaComponent
import com.exponea.sdk.models.ExponeaConfiguration

interface FileManager {
    fun getJsonOfFile(filename: String): String?
    fun getConfigurationOfFile(filename: String): ExponeaConfiguration
}