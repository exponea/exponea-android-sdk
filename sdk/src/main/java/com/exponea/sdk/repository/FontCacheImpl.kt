package com.exponea.sdk.repository

import android.content.Context

internal class FontCacheImpl(context: Context) : SimpleFileCache(context, DIRECTORY) {
    companion object {
        const val DIRECTORY = "exponeasdk_fonts_storage"
    }
}
