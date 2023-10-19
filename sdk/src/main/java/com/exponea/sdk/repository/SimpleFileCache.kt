package com.exponea.sdk.repository

import android.content.Context
import com.exponea.sdk.util.Logger
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal open class SimpleFileCache(context: Context, directoryPath: String) {
    companion object {
        private const val DOWNLOAD_TIMEOUT_SECONDS = 10L
    }

    private val httpClient = OkHttpClient.Builder()
            .callTimeout(DOWNLOAD_TIMEOUT_SECONDS, SECONDS)
            .build()
    private val directory: File = File(context.cacheDir, directoryPath)
    init {
        if (!directory.exists()) {
            directory.mkdir()
        }
    }

    fun getFileName(url: String): String {
        return MessageDigest
            .getInstance("SHA-512")
            .digest(url.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    fun clearExcept(urls: List<String>) {
        val keepFileNames = HashSet(urls.map { getFileName(it) })
        val files = try {
            directory.listFiles()
        } catch (e: Exception) {
            Logger.e(this, "Unable to access ${directory.path}, please validate storage permissions", e)
            null
        }
        files?.forEach { file ->
            if (!keepFileNames.contains(file.name)) {
                file.delete()
            }
        }
    }

    fun preload(urls: List<String>, callback: ((Boolean) -> Unit)?) {
        if (urls.isEmpty()) {
            callback?.invoke(true)
            return
        }
        val counter = AtomicInteger(urls.size)
        val downloadQueue = mutableListOf<Call>()
        val perFileCallback: ((Boolean) -> Unit) = { downloaded ->
            if (downloaded && counter.decrementAndGet() <= 0) {
                // this and ALL files are downloaded
                callback?.invoke(true)
            } else if (!downloaded) {
                // this file has not been downloaded -> global failure
                callback?.invoke(false)
                // stop downloading of others, there is no point to finish it
                downloadQueue.forEach { downloadRequest ->
                    try {
                        downloadRequest.cancel()
                    } catch (e: Exception) {
                        // silent close
                    }
                }
            }
        }
        for (fileUrl in urls) {
            if (has(fileUrl)) {
                counter.getAndDecrement()
                perFileCallback.invoke(true)
            } else {
                downloadFile(fileUrl, perFileCallback)?.let {
                    downloadQueue.add(it)
                }
            }
        }
    }

    fun downloadFile(url: String, callback: ((Boolean) -> Unit)?): Call? {
        val validUrl = url.toHttpUrlOrNull()
        if (validUrl == null) {
            callback?.invoke(false)
            return null
        }
        val request = Request.Builder().url(validUrl).build()
        val downloadRequest = httpClient.newCall(request)
        downloadRequest.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val file = createTempFile()
                    with(file.outputStream()) {
                        response.body?.byteStream()?.copyTo(this)
                        this.close()
                    }
                    file.renameTo(File(directory, getFileName(url)))
                    callback?.invoke(true)
                } else {
                    Logger.w(
                        this,
                        "Error while downloading file. Server responded ${response.code}"
                    )
                    callback?.invoke(false)
                }
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                Logger.w(this, "Error while downloading file from $url : $e")
                callback?.invoke(false)
            }
        })
        return downloadRequest
    }

    fun has(url: String): Boolean {
        return File(directory, getFileName(url)).exists()
    }

    fun getFile(url: String): File {
        return File(directory, getFileName(url))
    }
}
