package com.exponea.sdk.repository

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.exponea.sdk.repository.SimpleFileCache.Companion.DOWNLOAD_TIMEOUT_SECONDS
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.ensureOnMainThread
import com.exponea.sdk.util.runOnMainThread
import freeze.coil.ImageLoader
import freeze.coil.decode.GifDecoder
import freeze.coil.decode.ImageDecoderDecoder
import freeze.coil.decode.SvgDecoder
import freeze.coil.request.CachePolicy
import freeze.coil.request.ImageRequest
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class DrawableCacheImpl(
    private val context: Context
) : DrawableCache {

    companion object {
        const val DIRECTORY = "exponeasdk_img_storage"
    }

    internal val fileCache = SimpleFileCache(context, DIRECTORY)

    private val imageLoader = ImageLoader.Builder(context)
        .componentRegistry {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder(context))
            } else {
                add(GifDecoder())
            }
            add(SvgDecoder(context))
        }
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.DISABLED)
        .networkCachePolicy(CachePolicy.DISABLED)
        .build()

    override fun showImage(url: String?, target: ImageView, onImageNotLoaded: ((ImageView) -> Unit)?) {
        fun onImageNotLoadedFallback(reason: String) {
            Logger.d(this, reason)
            onImageNotLoaded?.let {
                Logger.d(this, "$reason, fallback to onImageNotLoaded")
                runOnMainThread { it.invoke(target) }
            }
        }
        if (url.isNullOrBlank()) {
            ensureOnMainThread {
                target.visibility = View.GONE
            }
            return
        }
        ensureOnBackgroundThread {
            fileCache.preload(url) { loaded ->
                if (loaded) {
                    ensureOnBackgroundThread {
                        val imageToShow = getFile(url)
                        if (imageToShow != null) {
                            runOnMainThread {
                                val request = ImageRequest.Builder(context)
                                    .data(imageToShow)
                                    .target(target)
                                    .listener(
                                        onError = { _, result ->
                                            onImageNotLoadedFallback(
                                                "Image showing failed due error: ${result.localizedMessage}"
                                            )
                                        }
                                    )
                                .build()
                                imageLoader.enqueue(request)
                                target.visibility = View.VISIBLE
                            }
                        } else {
                            onImageNotLoadedFallback("Image has not been found after preload")
                        }
                    }
                } else {
                    onImageNotLoadedFallback("Image has not been preloaded successfully")
                }
            }
        }
    }

    override fun getDrawable(url: String?): Drawable? {
        if (url.isNullOrBlank()) {
            return null
        }
        var result: Drawable? = null
        val semaphore = CountDownLatch(1)
        preload(listOf(url)) {
            val imageFile = getFile(url)
            if (imageFile == null) {
                semaphore.countDown()
                return@preload
            }
            imageLoader.enqueue(ImageRequest.Builder(context)
                .data(imageFile)
                .target(
                    onSuccess = { loadedDrawable ->
                        result = loadedDrawable
                        semaphore.countDown()
                    },
                    onError = { loadedDrawable ->
                        result = loadedDrawable
                        semaphore.countDown()
                    }
                )
                .build())
        }
        semaphore.await(DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        return result
    }

    override fun getDrawable(resourceId: Int): Drawable? {
        return AppCompatResources.getDrawable(context, resourceId)
    }

    override fun clear() = fileCache.clear()

    override fun getFile(url: String): File? = fileCache.getFile(url)

    override fun has(url: String): Boolean = fileCache.has(url)

    override fun preload(urls: List<String>, callback: ((Boolean) -> Unit)?) = fileCache.preload(urls, callback)
}
