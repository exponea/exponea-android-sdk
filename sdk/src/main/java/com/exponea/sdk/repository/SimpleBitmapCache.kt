package com.exponea.sdk.repository

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.exponea.sdk.util.Logger
import kotlin.math.max
import kotlin.math.min

internal open class SimpleBitmapCache(
    context: Context,
    directoryPath: String
) : SimpleFileCache(context, directoryPath) {

    /**
     * Retrieves file as Bitmap if possible.
     */
    fun getBitmap(url: String): Bitmap? {
        if (!has(url)) {
            return null
        }
        try {
            val filePath = getFile(url).path
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
