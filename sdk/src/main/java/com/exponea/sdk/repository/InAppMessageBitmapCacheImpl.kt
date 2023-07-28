package com.exponea.sdk.repository

import android.content.Context
import android.graphics.Bitmap

internal class InAppMessageBitmapCacheImpl(
    context: Context
) : SimpleBitmapCache(context, DIRECTORY), BitmapCache {

    companion object {
        const val DIRECTORY = "exponeasdk_in_app_message_storage"
    }

    override fun get(url: String): Bitmap? {
        return super.getBitmap(url)
    }
}
