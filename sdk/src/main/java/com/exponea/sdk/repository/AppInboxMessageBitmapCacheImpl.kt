package com.exponea.sdk.repository

import android.content.Context

internal class AppInboxMessageBitmapCacheImpl(
    context: Context
) : SimpleDrawableCache(context, DIRECTORY) {
    companion object {
        const val DIRECTORY = "exponeasdk_app_inbox_message_storage"
    }
}
