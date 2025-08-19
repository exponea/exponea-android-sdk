package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.runOnBackgroundThread
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleFileCacheTest {

    private val sameHostImageUrls = listOf(
        "https://www.loc.gov/static/portals/free-to-use/public-domain/abrahamlincoln/image1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/arab-americans/arab-americans-1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/african-american-women-changemakers/10.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/aircraft/aircraft-1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/american-revolution/revolution-founding-1.jpg", // ktlint-disable max-line-length
        "https://www.loc.gov/static/portals/free-to-use/public-domain/architecture-and-design/12845v.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/artists-and-photographers/artists-photographers-1.jpg", // ktlint-disable max-line-length
        "https://www.loc.gov/static/portals/free-to-use/public-domain/art-of-the-book/art-of-the-book-1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/asian-american-pacific-islander-heritage/AAPI-38.jpg", // ktlint-disable max-line-length
        "https://www.loc.gov/static/portals/free-to-use/public-domain/athletes/athletes-1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/autumn-and-halloween/autumn-1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/birthdays/birthdays-1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/cars/1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/baseball-cards/2_1407fv.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/birds/birds-4.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/bicycles/03-3d01840v.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/bridges/1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/colors-tell-the-story/color-1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/classic-childrens-books/1.jpg",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/cats/33.jpg"
    )

    private val uniqueImageUrls = listOf(
        "https://upload.wikimedia.org/wikipedia/commons/thumb/6/63/Wikipedia-logo.png/800px-Wikipedia-logo.png",
        "https://www.w3.org/Icons/w3c_home",
        "https://www.python.org/static/community_logos/python-logo.png",
        "https://nodejs.org/static/images/logo.svg",
        "https://www.rust-lang.org/logos/rust-logo-512x512.png",
        "https://cdn.alza.cz/images/web-static/eshop-logos/alza_sk.svg",
        "https://www.apache.org/img/asf_logo.png",
        "https://www.mozilla.org/media/img/logos/m24/lockup-black.f2ddba3f0724.svg",
        "https://assets.ubuntu.com/v1/29985a98-ubuntu-logo32.png",
        "https://www.debian.org/logos/openlogo-nd-50.png",
        "https://www.kernel.org/theme/images/logos/tux.png",
        "https://www.freebsd.org/images/banner-red.png",
        "https://www.loc.gov/static/portals/free-to-use/public-domain/abrahamlincoln/image1.jpg",
        "https://www.r-project.org/Rlogo.png",
        "https://go.dev/images/gophers/ladder.svg",
        "https://www.postgresql.org/media/img/about/press/elephant.png",
        "https://www.sqlite.org/images/sqlite370_banner.gif",
        "https://nginx.org/nginx.png",
        "https://curl.se/logo/curl-logo.svg",
        "https://archive.org/images/glogo.png"
    )

    @Before
    fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, DrawableCacheImpl.DIRECTORY).deleteRecursively()
    }

    @After
    fun cleanDownloads() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, DrawableCacheImpl.DIRECTORY).deleteRecursively()
    }

    @Test
    fun `should download single image`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        val downloadCount = CountDownLatch(sameHostImageUrls.size + uniqueImageUrls.size)
        (sameHostImageUrls + uniqueImageUrls).forEach { cache.preload(it) { status ->
            assertTrue(status, "Image `$it` failed to download")
            downloadCount.countDown()
        } }
        assertTrue(
            downloadCount.await(10, TimeUnit.SECONDS),
            "Few (${downloadCount.count + 1}) images has not been downloaded yet"
        )
    }

    @Test
    fun `should download real image by same thread`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageUrl = """
            https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg/1200px-European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg
        """.trimIndent()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        var downloaded = false
        waitForIt {
            cache.preload(imageUrl) { status ->
                downloaded = status
                it()
            }
        }
        assertTrue(cache.has(imageUrl))
        assertTrue(downloaded)
        assertNotNull(cache.getFile(imageUrl))
    }

    @Test
    fun `should download real image by background thread`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageUrl = """
            https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg/1200px-European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg
        """.trimIndent()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        var downloaded = false
        waitForIt(SimpleFileCache.DOWNLOAD_TIMEOUT_SECONDS * 1000L) {
            cache.preload(listOf(imageUrl)) { status ->
                downloaded = status
                it()
            }
        }
        assertTrue(cache.has(imageUrl))
        assertTrue(downloaded)
        assertNotNull(cache.getFile(imageUrl))
    }

    @Test
    fun `should download real image only once`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val imageUrl = """
            https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg/1200px-European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg
        """.trimIndent()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        val downloadedCount = AtomicInteger(0)
        val downloadAttempts = 1000
        waitForIt(SimpleFileCache.DOWNLOAD_TIMEOUT_SECONDS * 1000L) {
            for (i in 0 until downloadAttempts) {
                runOnBackgroundThread {
                    cache.preload(listOf(imageUrl)) {
                        if (downloadedCount.incrementAndGet() == downloadAttempts) {
                            it()
                        }
                    }
                }
            }
        }
        assertTrue(cache.has(imageUrl))
        assertNotNull(cache.getFile(imageUrl))
        assertEquals(downloadAttempts, downloadedCount.get())
    }

    @Test
    fun `should download real image only once - 404 test`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invalidImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/non-existing-image.jpg"
        val imageUrl = """
            https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg/1200px-European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg
        """.trimIndent()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        val downloadedCount = AtomicInteger(0)
        val downloadAttempts = 1000
        waitForIt(SimpleFileCache.DOWNLOAD_TIMEOUT_SECONDS * 1000L) {
            for (i in 0 until downloadAttempts) {
                runOnBackgroundThread {
                    val imageUrlToDownload = if (i == 0) {
                        invalidImageUrl
                    } else {
                        imageUrl
                    }
                    cache.preload(listOf(imageUrlToDownload)) {
                        if (downloadedCount.incrementAndGet() == downloadAttempts) {
                            it()
                        }
                    }
                }
            }
        }
        assertTrue(cache.has(imageUrl))
        assertNotNull(cache.getFile(imageUrl))
        assertEquals(downloadAttempts, downloadedCount.get())
    }

    @Test
    fun `should continue with images downloading if one is corrupted`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invalidImageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/non-existing-image.jpg"
        val imagesToDownload = uniqueImageUrls.toMutableList().apply {
            add(2, invalidImageUrl)
        }
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        waitForIt(SimpleFileCache.DOWNLOAD_TIMEOUT_SECONDS * 1000L) { done ->
            cache.preload(imagesToDownload) { status ->
                // failure is OK here
                assertFalse(status)
                done()
            }
        }
    }

    @Test
    fun `should download multiple real images by background thread`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        cache.clear()
        val imageUrls = uniqueImageUrls
        assertEquals(20, imageUrls.distinct().size)
        var downloaded = false
        waitForIt(SimpleFileCache.DOWNLOAD_TIMEOUT_SECONDS * 1000L) {
            cache.preload(imageUrls) { status ->
                downloaded = status
                it()
            }
        }
        assertTrue(downloaded)
        imageUrls.forEach {
            assertTrue(cache.has(it))
            assertNotNull(cache.getFile(it))
        }
    }

    @Test
    fun `should download multiple real images from on same hostname by background thread`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        val imageUrls = sameHostImageUrls
        assertEquals(20, imageUrls.distinct().size)
        var downloaded = false
        waitForIt(SimpleFileCache.DOWNLOAD_TIMEOUT_SECONDS * 1000L) {
            cache.preload(imageUrls) { status ->
                downloaded = status
                it()
            }
        }
        assertTrue(downloaded)
        imageUrls.forEach {
            assertTrue(cache.has(it))
            assertNotNull(cache.getFile(it))
        }
    }

    @Test
    fun `should download multiple real images from bloomreach cloud`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        val imageUrls = listOf(
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/eb112162-ab0b-11ef-8cb8-1a8430f8de86", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/180056e0-21ad-11ef-b308-468745c46878", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/7922959a-37d4-11ef-a80b-7e9a3cdef6a6", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/61018f92-37d5-11ef-bfbc-166b84f29207", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/229bdac8-2f06-11ef-b469-e67ce79d3eca", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/4f3efcd8-c753-11ee-aafe-1ef53dcbe20a" // ktlint-disable max-line-length
        )
        assertEquals(6, imageUrls.distinct().size)
        var downloaded = false
        waitForIt(SimpleFileCache.DOWNLOAD_TIMEOUT_SECONDS * 1000L) {
            cache.preload(imageUrls) { status ->
                downloaded = status
                it()
            }
        }
        assertTrue(downloaded)
        imageUrls.forEach {
            assertTrue(cache.has(it))
            assertNotNull(cache.getFile(it))
        }
    }

    @Test
    fun `should download multiple real images from bloomreach cloud by multiple threads`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        val imageUrls = listOf(
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/eb112162-ab0b-11ef-8cb8-1a8430f8de86", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/180056e0-21ad-11ef-b308-468745c46878", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/7922959a-37d4-11ef-a80b-7e9a3cdef6a6", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/61018f92-37d5-11ef-bfbc-166b84f29207", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/229bdac8-2f06-11ef-b469-e67ce79d3eca", // ktlint-disable max-line-length
            "https://brxcdn.com/c7s-app-storage/b556af1a-bf4e-11ed-ac28-de4945357d1a/media/original/4f3efcd8-c753-11ee-aafe-1ef53dcbe20a" // ktlint-disable max-line-length
        )
        assertEquals(6, imageUrls.distinct().size)
        val downloadedCount = CountDownLatch(imageUrls.size)
        imageUrls.forEach { imageUrl ->
            runOnBackgroundThread {
                cache.preload(listOf(imageUrl)) { status ->
                    downloadedCount.countDown()
                }
            }
        }
        assertTrue(downloadedCount.await(SimpleFileCache.DOWNLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        imageUrls.forEach {
            assertTrue(cache.has(it))
            assertNotNull(cache.getFile(it))
        }
    }
}
