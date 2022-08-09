package com.exponea.sdk.repository

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.exponea.sdk.util.Logger
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal class InAppMessageBitmapCacheImpl(context: Context) : InAppMessageBitmapCache {
    companion object {
        const val DIRECTORY = "exponeasdk_in_app_message_storage"
        private const val DOWNLOAD_TIMEOUT_SECONDS = 10L
    }

    private val httpClient = OkHttpClient.Builder()
            .callTimeout(DOWNLOAD_TIMEOUT_SECONDS, SECONDS)
            .build()
    private val directory: File = File(context.cacheDir, DIRECTORY)
    init {
        if (!directory.exists()) {
            directory.mkdir()
        }
    }

    fun getFileName(url: String): String {
        return Base64.encodeToString(url.toByteArray(), Base64.NO_WRAP).replace("=", "").replace("/", "-")
    }

    override fun clearExcept(urls: List<String>) {
        val keepFileNames = HashSet(urls.map { getFileName(it) })
        val files = directory.listFiles()
        files?.forEach { file ->
            if (!keepFileNames.contains(file.name)) {
                file.delete()
            }
        }
    }

    override fun preload(urls: List<String>, callback: ((Boolean) -> Unit)?) {
        if (urls.isEmpty()) {
            callback?.invoke(true)
            return
        }
        val counter = AtomicInteger(urls.size)
        val downloadQueue = mutableListOf<Call>()
        val perImageCallback: ((Boolean) -> Unit) = { downloaded ->
            if (downloaded && counter.decrementAndGet() <= 0) {
                // this and ALL images are downloaded
                callback?.invoke(true)
            } else if (!downloaded) {
                // this image has not been downloaded -> global failure
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
        for (imageUrl in urls) {
            if (has(imageUrl)) {
                counter.getAndDecrement()
                perImageCallback.invoke(true)
            } else {
                downloadQueue.add(downloadImage(imageUrl, perImageCallback))
            }
        }
    }

    fun downloadImage(url: String, callback: ((Boolean) -> Unit)?): Call {
        val request = Request.Builder().url(url).build()
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
                        "Error while preloading in-app message image. Server responded ${response.code}"
                    )
                    callback?.invoke(false)
                }
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                Logger.w(this, "Error while preloading in-app message image $e")
                callback?.invoke(false)
            }
        })
        return downloadRequest
    }

    override fun has(url: String): Boolean {
        return File(directory, getFileName(url)).exists()
    }

    override fun get(url: String): Bitmap? {
        if (!has(url)) {
            return null
        }
        try {
            val filePath = File(directory, getFileName(url)).path
            val boundsOnlyOptions = BitmapFactory.Options()
            boundsOnlyOptions.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, boundsOnlyOptions)
            val scaleDownOptions = BitmapFactory.Options()
            scaleDownOptions.inSampleSize = calculateSampleSize(boundsOnlyOptions.outWidth, boundsOnlyOptions.outHeight)
            return BitmapFactory.decodeFile(filePath, scaleDownOptions)
        } catch (e: Throwable) {
            Logger.w(this, "Unable to load bitmap $e")
            return null
        }
    }

    /**
     * Calculates sample size for bitmap factory. Sample size means "take every X pixel" from original.
     * We only need the image in resolution of the screen.
     * So in portrait width up to width of screen, height up to height of screen. Similar in landscape.
     */
    private fun calculateSampleSize(bitmapWidth: Int, bitmapHeight: Int): Int {
        try {
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels

            val portraitRatio = max(bitmapWidth / screenWidth, bitmapHeight / screenHeight)
            val landscapeRatio = max(bitmapWidth / screenHeight, bitmapHeight / screenWidth)
            return max(1, min(portraitRatio, landscapeRatio))
        } catch (e: Throwable) {
            Logger.w(this, "Unable to calculate bitmap sample size $e")
            return 1
        }
    }
}
