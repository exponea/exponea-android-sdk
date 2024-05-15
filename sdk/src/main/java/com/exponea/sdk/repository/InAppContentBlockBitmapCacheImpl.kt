package com.exponea.sdk.repository

import android.content.Context

internal class InAppContentBlockBitmapCacheImpl(
    context: Context
) : SimpleDrawableCache(context, DIRECTORY) {
    companion object {
        const val DIRECTORY = "exponeasdk_inapp_content_blocks_storage"
    }
}
