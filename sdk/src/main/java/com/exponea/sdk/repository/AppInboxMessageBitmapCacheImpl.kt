package com.exponea.sdk.repository

import android.content.Context
import android.graphics.Bitmap

internal class AppInboxMessageBitmapCacheImpl(
    context: Context
) : SimpleBitmapCache(context, DIRECTORY), BitmapCache {

    companion object {
        const val DIRECTORY = "exponeasdk_app_inbox_message_storage"
    }

    override fun get(url: String): Bitmap? {
        return super.getBitmap(url)
    }
}
