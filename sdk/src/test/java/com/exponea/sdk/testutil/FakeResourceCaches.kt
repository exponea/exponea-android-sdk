package com.exponea.sdk.testutil

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.repository.FontCacheImpl
import io.mockk.every
import java.io.File

/**
 * Font file bundled in test resources, used instead of downloading fonts from the network.
 */
internal fun testFontFile(): File =
    File(FakeFontCache::class.java.classLoader!!.getResource("xtrusion.ttf")!!.toURI())

/**
 * Smallest valid PNG image (1x1 pixel), used instead of downloading images from the network.
 */
internal fun testPngBytes(): ByteArray = java.util.Base64.getDecoder().decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
)

/**
 * Image file with the [testPngBytes] content in the app files directory, used as a cached image.
 */
internal fun testImageFile(): File =
    File(ApplicationProvider.getApplicationContext<Context>().filesDir, "test_image.png").apply {
        if (!exists()) {
            writeBytes(testPngBytes())
        }
    }

/**
 * Constructor-mocks [FontCacheImpl], so every instance serves [testFontFile] and reports all fonts as loaded.
 * Use it where the code under test creates its own font cache; inject [FakeFontCache] where it can be passed in.
 */
internal fun prepareFontCacheMocks() {
    mockkConstructorFix(FontCacheImpl::class)
    every { anyConstructed<FontCacheImpl>().getFontFile(any()) } returns testFontFile()
    every { anyConstructed<FontCacheImpl>().getTypeface(any()) } returns Typeface.createFromFile(testFontFile())
    every { anyConstructed<FontCacheImpl>().has(any()) } returns true
    every {
        anyConstructed<FontCacheImpl>().preload(any<List<String>>(), any())
    } answers {
        secondArg<((Boolean) -> Unit)?>()?.invoke(true)
    }
}

/**
 * [FontCache] that serves the bundled test font for any URL and records which URLs were requested.
 */
internal class FakeFontCache : FontCache {
    val requestedUrls = mutableListOf<String>()

    override fun getTypeface(url: String): Typeface? {
        requestedUrls.add(url)
        return Typeface.createFromFile(testFontFile())
    }

    override fun getFontFile(url: String): File? = testFontFile()

    override fun preload(urls: List<String>, callback: ((Boolean) -> Unit)?) {
        callback?.invoke(true)
    }

    override fun has(url: String): Boolean = true

    override fun clear() = Unit
}

/**
 * [DrawableCache] that models a cache already holding every requested image: [preload] and [has] succeed,
 * [getFile] returns [testImageFile] and [getDrawable] returns a placeholder drawable.
 */
internal class FakeDrawableCache : DrawableCache {
    override fun preload(urls: List<String>, callback: ((Boolean) -> Unit)?) {
        callback?.invoke(true)
    }

    override fun has(url: String): Boolean = true

    override fun clear() = Unit

    override fun getFile(url: String): File? = testImageFile()

    override fun showImage(url: String?, target: ImageView, onImageNotLoaded: ((ImageView) -> Unit)?) = Unit

    override fun getDrawable(url: String?): Drawable? {
        if (url.isNullOrBlank()) {
            return null
        }
        return ColorDrawable()
    }

    override fun getDrawable(resourceId: Int): Drawable? = ColorDrawable()
}
