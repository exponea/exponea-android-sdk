package com.exponea.sdk.repository

import android.graphics.Typeface
import androidx.annotation.WorkerThread
import java.io.File

interface FontCache {
    @WorkerThread
    fun getTypeface(url: String): Typeface?
    fun getFontFile(url: String): File?
    fun preload(urls: List<String>, callback: ((Boolean) -> Unit)?)
    fun has(url: String): Boolean
    fun clear()
}
