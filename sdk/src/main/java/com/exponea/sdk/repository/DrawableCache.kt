package com.exponea.sdk.repository

import android.graphics.drawable.Drawable
import android.widget.ImageView
import java.io.File

internal interface DrawableCache {
    fun preload(urls: List<String>, callback: ((Boolean) -> Unit)? = null)
    fun has(url: String): Boolean
    fun clear()
    fun getFile(url: String): File?
    fun showImage(url: String?, target: ImageView, onImageNotLoaded: ((ImageView) -> Unit)? = null)
    fun getDrawable(url: String?): Drawable?
    fun getDrawable(resourceId: Int): Drawable?
}
