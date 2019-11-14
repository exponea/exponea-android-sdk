package com.exponea.sdk.manager

internal interface FileManager {
    fun createFile(filename: String, type: String)
    fun writeToFile(filename: String, text: String)
}
