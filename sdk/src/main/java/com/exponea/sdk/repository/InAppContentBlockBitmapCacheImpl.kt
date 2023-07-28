package com.exponea.sdk.repository

import android.content.Context
import android.graphics.Bitmap

internal class InAppContentBlockBitmapCacheImpl(
    context: Context
) : SimpleBitmapCache(context, DIRECTORY), BitmapCache {

    companion object {
        const val DIRECTORY = "exponeasdk_inapp_content_blocks_storage"
    }

    override fun get(url: String): Bitmap? {
        return super.getBitmap(url)
    }
}
