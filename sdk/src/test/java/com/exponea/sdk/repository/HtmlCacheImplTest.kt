package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.BuildConfig
import com.exponea.sdk.preferences.ExponeaPreferencesImpl
import com.exponea.sdk.util.ExponeaGson
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.HtmlNormalizer.HtmlNormalizerConfig
import com.exponea.sdk.util.HtmlNormalizer.NormalizedResult
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HtmlCacheImplTest {

    private lateinit var cache: HtmlNormalizedCache
    private lateinit var imageCache: DrawableCache
    private lateinit var fontCache: FontCache

    @Before
    fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        imageCache = DrawableCacheImpl(context)
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
        assertFalse(fileCache.retrieveFileDirectly("InAppContentBlock_cached_$key.json").exists())
    }

    @Test
    fun `should skip html store for invalid normalisation`() {
        val originalHtml = "<body><div>Hello</div></body>"
        val normalizedHtml = normalizeHtml(originalHtml)
        normalizedHtml.valid = false
        val key = "key"
        cache.set(key, originalHtml, normalizedHtml)
        // verify
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileCache = SimpleFileCache(context, "exponeasdk_html_storage")
        assertFalse(fileCache.retrieveFileDirectly("InAppContentBlock_cached_$key.json").exists())
        assertNull(cache.get(key, originalHtml))
    }

    @Test
    fun `should remove file with missing version`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileCache = SimpleFileCache(context, "exponeasdk_html_storage")
        val key = "key"
        val fileName = "InAppContentBlock_cached_$key.json"
        // previously stored html result cache without version schema
        val originalHtml = "<body><div>Hello</div></body>"
        val normalizedHtml = normalizeHtml(originalHtml)
        val normalizedJson = ExponeaGson.instance.toJson(normalizedHtml)
        // stores all metadata
        cache.set(key, originalHtml, normalizedHtml)
        // rewrites content to simulate old version
        fileCache.retrieveFileDirectly(fileName).writeText(normalizedJson)
        // try to get html result
        assertTrue(fileCache.retrieveFileDirectly(fileName).exists())
        assertNull(cache.get(key, originalHtml))
        assertFalse(fileCache.retrieveFileDirectly(fileName).exists())
    }

    @Test
    fun `should remove file with older version`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileCache = SimpleFileCache(context, "exponeasdk_html_storage")
        val key = "key"
        val fileName = "InAppContentBlock_cached_$key.json"
        // stored html result cache with older version
        val originalHtml = "<body><div>Hello</div></body>"
        val normalizedHtml = normalizeHtml(originalHtml)
        val versionedResult = HtmlNormalizedCacheImpl.VersionedNormalizedResult("4.0.0", normalizedHtml)
        val versionedResultJson = ExponeaGson.instance.toJson(versionedResult)
        // stores all metadata
        cache.set(key, originalHtml, normalizedHtml)
        // rewrites to simulate old version
        fileCache.retrieveFileDirectly(fileName).writeText(versionedResultJson)
        // try to get html result
        assertTrue(fileCache.retrieveFileDirectly(fileName).exists())
        assertNull(cache.get(key, originalHtml))
        assertFalse(fileCache.retrieveFileDirectly(fileName).exists())
    }

    @Test
    fun `should remove file with newer version`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileCache = SimpleFileCache(context, "exponeasdk_html_storage")
        val key = "key"
        val fileName = "InAppContentBlock_cached_$key.json"
        // stored html result cache with older version
        val originalHtml = "<body><div>Hello</div></body>"
        val normalizedHtml = normalizeHtml(originalHtml)
        val versionedResult = HtmlNormalizedCacheImpl.VersionedNormalizedResult("999.0.0", normalizedHtml)
        val versionedResultJson = ExponeaGson.instance.toJson(versionedResult)
        // stores all metadata
        cache.set(key, originalHtml, normalizedHtml)
        // rewrites to simulate newer version
        fileCache.retrieveFileDirectly(fileName).writeText(versionedResultJson)
        // try to get html result
        assertTrue(fileCache.retrieveFileDirectly(fileName).exists())
        assertNull(cache.get(key, originalHtml))
        assertFalse(fileCache.retrieveFileDirectly(fileName).exists())
    }

    @Test
    fun `should get file with same version`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fileCache = SimpleFileCache(context, "exponeasdk_html_storage")
        val key = "key"
        val fileName = "InAppContentBlock_cached_$key.json"
        // stored html result cache with older version
        val originalHtml = "<body><div>Hello</div></body>"
        val normalizedHtml = normalizeHtml(originalHtml)
        val versionedResult = HtmlNormalizedCacheImpl.VersionedNormalizedResult(
            BuildConfig.EXPONEA_VERSION_NAME,
            normalizedHtml
        )
        val versionedResultJson = ExponeaGson.instance.toJson(versionedResult)
        // stores all metadata
        cache.set(key, originalHtml, normalizedHtml)
        // rewrites to mitigate
        fileCache.retrieveFileDirectly(fileName).writeText(versionedResultJson)
        // try to get html result
        assertTrue(fileCache.retrieveFileDirectly(fileName).exists())
        assertNotNull(cache.get(key, originalHtml))
        assertTrue(fileCache.retrieveFileDirectly(fileName).exists())
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
