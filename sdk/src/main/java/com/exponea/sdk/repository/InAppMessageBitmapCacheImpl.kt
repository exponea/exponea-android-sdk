package com.exponea.sdk.repository

import android.content.Context

internal class InAppMessageBitmapCacheImpl(
    context: Context
) : SimpleDrawableCache(context, DIRECTORY) {
    companion object {
        const val DIRECTORY = "exponeasdk_in_app_message_storage"
    }
}
