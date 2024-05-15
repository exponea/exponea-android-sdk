package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.HtmlNormalizer.HtmlNormalizerConfig
import com.exponea.sdk.util.HtmlNormalizer.NormalizedResult
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HtmlCacheImplTest {

    private lateinit var cache: HtmlNormalizedCache
    private lateinit var imageCache: DrawableCache
    private lateinit var fontCache: SimpleFileCache

    @Before
    fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        imageCache = InAppMessageBitmapCacheImpl(context)
        cache = HtmlNormalizedCacheImpl(context, ExponeaPreferencesImpl(context))
        fontCache = FontCacheImpl(context)
    }

    @After
    fun after() {
        // remove all keys that are used in tests
        cache.remove("key")
    }

    @Test
    fun `should store html`() {
        val originalHtml = "<body><div>Hello</div></body>"
        val normalizedHtml = normalizeHtml(originalHtml)
        val key = "key"
        cache.set(key, originalHtml, normalizedHtml)
        val cachedHtmlResult = cache.get(key, originalHtml)
        assertNotNull(cachedHtmlResult)
        assertNotNull(cachedHtmlResult.html)
        assertEquals(normalizedHtml.html, cachedHtmlResult.html)
    }

    @Test
    fun `should return null cache because of different html`() {
        val originalHtml1 = "<body><div>Hello</div></body>"
        val originalHtml2 = "<body><div>There</div></body>"
        val normalizedHtml = normalizeHtml(originalHtml1)
        val key = "key"
        cache.set(key, originalHtml1, normalizedHtml)
        val cachedHtmlResult2 = cache.get(key, originalHtml2)
        assertNull(cachedHtmlResult2)
        val cachedHtmlResult1 = cache.get(key, originalHtml1)
        assertNotNull(cachedHtmlResult1)
    }

    @Test
    fun `should update html`() {
        val originalHtml1 = "<body><div>Hello</div></body>"
        val originalHtml2 = "<body><div>There</div></body>"
        val normalizedHtml1 = normalizeHtml(originalHtml1)
        val normalizedHtml2 = normalizeHtml(originalHtml2)
        val key = "key"
        cache.set(key, originalHtml1, normalizedHtml1)
        assertNotNull(cache.get(key, originalHtml1))
        cache.set(key, originalHtml2, normalizedHtml2)
        assertNull(cache.get(key, originalHtml1))
        assertNotNull(cache.get(key, originalHtml2))
    }

    @Test
    fun `should remove cache when file is corrupted`() {
        val originalHtml = "<body><div>Hello</div></body>"
        val normalizedHtml = normalizeHtml(originalHtml)
        val key = "key"
        cache.set(key, originalHtml, normalizedHtml)
        // file corruption
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileCache = SimpleFileCache(context, "exponeasdk_html_storage")
        fileCache.retrieveFileDirectly("InAppContentBlock_cached_$key.json").writeText("{{{")
        // verify
        assertNull(cache.get(key, originalHtml))
    }

    private fun normalizeHtml(originalHtml: String): NormalizedResult {
        return HtmlNormalizer(
            imageCache, fontCache, originalHtml
        ).normalize(HtmlNormalizerConfig(
            makeResourcesOffline = false,
            ensureCloseButton = false
        ))
    }
}
