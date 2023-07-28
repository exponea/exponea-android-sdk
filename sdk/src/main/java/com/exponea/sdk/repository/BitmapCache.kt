package com.exponea.sdk.repository

import android.graphics.Bitmap

internal interface BitmapCache {
    fun preload(urls: List<String>, callback: ((Boolean) -> Unit)? = null)
    fun has(url: String): Boolean
    fun get(url: String): Bitmap?
    fun clearExcept(urls: List<String>)
}
