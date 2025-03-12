package com.exponea.sdk.repository

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.WorkerThread
import com.exponea.sdk.util.Logger
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class FontCacheImpl(context: Context) : SimpleFileCache(context, DIRECTORY), FontCache {
    @WorkerThread
    override fun getTypeface(url: String): Typeface? {
        var fontFile = getFontFile(url)
        if (fontFile == null) {
            val downloadAwait = CountDownLatch(1)
            preload(listOf(url)) {
                downloadAwait.countDown()
            }
            downloadAwait.await(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            fontFile = getFontFile(url)
        }
        if (fontFile == null) {
            Logger.w(this, "Font has not been downloaded: $url")
            return null
        }
        try {
            return Typeface.createFromFile(fontFile)
        } catch (e: Exception) {
            Logger.e(this, "Typeface has not been created from $url")
            return null
        }
    }

    override fun getFontFile(url: String): File? {
        return super.getFile(url)
    }

    companion object {
        const val DIRECTORY = "exponeasdk_fonts_storage"
    }
}
