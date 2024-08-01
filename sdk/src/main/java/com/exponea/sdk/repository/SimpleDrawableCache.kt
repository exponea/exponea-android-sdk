package com.exponea.sdk.repository

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.widget.ImageView
import com.exponea.sdk.util.Logger
import com.exponea.sdk.util.ensureOnBackgroundThread
import com.exponea.sdk.util.runOnMainThread
import freeze.coil.ImageLoader
import freeze.coil.decode.GifDecoder
import freeze.coil.decode.ImageDecoderDecoder
import freeze.coil.request.CachePolicy
import freeze.coil.request.ImageRequest
import kotlin.math.max
import kotlin.math.min

internal open class SimpleDrawableCache(
    private val context: Context,
    directoryPath: String
) : SimpleFileCache(context, directoryPath), DrawableCache {

    private val imageLoader = ImageLoader.Builder(context)
        .componentRegistry {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder(context))
            } else {
                add(GifDecoder())
            }
        }
        .memoryCachePolicy(CachePolicy.DISABLED)
        .diskCachePolicy(CachePolicy.DISABLED)
        .networkCachePolicy(CachePolicy.DISABLED)
        .build()

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

    override fun showImage(url: String?, target: ImageView, onImageNotLoaded: ((ImageView) -> Unit)?) {
        fun onImageNotLoadedFallback(reason: String) {
            Logger.d(this, reason)
            onImageNotLoaded?.let {
                Logger.d(this, "$reason, fallback to onImageNotLoaded")
                runOnMainThread { it.invoke(target) }
            }
        }
        if (url.isNullOrBlank()) {
            onImageNotLoadedFallback("Image url is null")
            return
        }
        ensureOnBackgroundThread {
            preload(listOf(url)) { loaded ->
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
}
