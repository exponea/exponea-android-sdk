package com.exponea.sdk.repository

import android.widget.ImageView
import java.io.File

internal interface DrawableCache {
    fun preload(urls: List<String>, callback: ((Boolean) -> Unit)? = null)
    fun has(url: String): Boolean
    fun clearExcept(urls: List<String>)
    fun getFile(url: String): File?
    fun showImage(url: String?, target: ImageView, onImageNotLoaded: ((ImageView) -> Unit)? = null)
}
