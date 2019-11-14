package com.exponea.sdk.manager

import java.io.File

internal class FileManagerImpl : FileManager {

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
}
