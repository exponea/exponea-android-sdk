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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleFileCacheTest {

    @Before
    fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.cacheDir, DrawableCacheImpl.DIRECTORY).deleteRecursively()
    }

    @After
    fun cleanDownloads() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.cacheDir, DrawableCacheImpl.DIRECTORY).deleteRecursively()
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
    fun `should download multiple real images by background thread`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        cache.clear()
        val imageUrls = listOf(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg/1200px-European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg", // ktlint-disable max-line-length
            "https://c02.purpledshub.com/uploads/sites/47/2024/06/GettyImages-157195802-scaled.jpg",
            "https://jspcs-birdspikesonline-gob2b.b-cdn.net/imagecache/cbc76987-999f-4f36-9a9b-aedd00b311da/seagull-profile_500x500.jpg", // ktlint-disable max-line-length
            "https://as1.ftcdn.net/v2/jpg/05/73/63/78/1000_F_573637859_X1thrFZVpV6S4DDcj0SMZnIiQdIgs6TH.jpg",
            "https://scx2.b-cdn.net/gfx/news/hires/2024/seagulls.jpg",
            "https://birdaware.org/solent/wp-content/uploads/sites/2/2021/09/Great-black-backed-gull-HERO.png",
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRLH4vE8uByIqIC3RbyTR4a07Ri4gASexCfuQ&s",
            "https://static.wixstatic.com/media/135d23_dd53ee7c93e54bdc895707bbd8121492~mv2.png/v1/fill/w_560,h_444,al_c,q_85,usm_0.66_1.00_0.01,enc_avif,quality_auto/135d23_dd53ee7c93e54bdc895707bbd8121492~mv2.png", // ktlint-disable max-line-length
            "https://cdn11.bigcommerce.com/s-xj69ljw63/product_images/uploaded_images/seagull.jpeg",
            "https://falcones.co.uk/wp-content/uploads/2019/07/angry-2730_1280-e1577789500639-768x512.jpg",
            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSsZVt8Ds4PKpdkkYMWhTEhtRJZVBL4WovVZg&s",
            "https://images.squarespace-cdn.com/content/v1/5ab3de834eddec92938aef0a/1581961744889-AYNURHQY269SV3UOEXEK/seagull-4349143_960_720.jpg", // ktlint-disable max-line-length
            "https://www.abellpestcontrol.com/-/media/Abell/Pest/Pest-Images/Seagull/Pest-Images/900x906_Oct2019_Seagull3.jpg?rev=88b97bd538e84c6aa33c9445ed9c6152", // ktlint-disable max-line-length
            "https://images.squarespace-cdn.com/content/v1/5ab3de834eddec92938aef0a/1581961514040-HXCCNRXERJBZTXUDY6BS/IMG_8508.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4d/Gull_ca_usa.jpg/640px-Gull_ca_usa.jpg",
            "https://theconversationproject.org/wp-content/uploads/2023/11/Seagull-swooping-image2.jpg",
            "https://m.media-amazon.com/images/I/61yknJ33qhL._AC_UF1000,1000_QL80_.jpg",
            "https://www.trvst.world/wp-content/uploads/2023/07/seagull-during-daytime.jpg",
            "https://www.shamanism.one/wp-content/uploads/2024/02/Spirit-Animal-Seagull.webp",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/Larus_occidentalis_%28Western_Gull%29%2C_Point_Lobos%2C_CA%2C_US_-_May_2013.jpg/1200px-Larus_occidentalis_%28Western_Gull%29%2C_Point_Lobos%2C_CA%2C_US_-_May_2013.jpg" // ktlint-disable max-line-length
        )
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
        val imageUrls = listOf(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8a/European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg/440px-European_herring_gull_%28Larus_argentatus%29._Saint-Malo%2C_France.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/21/Armenian_Gull_Juvenile_in_flight%2C_Sevan_lake.jpg/500px-Armenian_Gull_Juvenile_in_flight%2C_Sevan_lake.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/73/Larus_pacificus_Bruny_Island.jpg/500px-Larus_pacificus_Bruny_Island.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a1/Swallow-tailed-gull.jpg/440px-Swallow-tailed-gull.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fd/Haugesund_komm.svg/260px-Haugesund_komm.svg.png", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/Seagull_eating_starfish.jpg/360px-Seagull_eating_starfish.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Gull_attacking_coot.jpg/360px-Gull_attacking_coot.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/8e/Huntington_beach_pier_seagull_2023.jpg/360px-Huntington_beach_pier_seagull_2023.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/27/Lesser_Black-backed_Gulls.jpg/360px-Lesser_Black-backed_Gulls.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0b/Sea_Gull_at_Point_Lobos_State_Natural_Reserve%2C_CA.jpg/500px-Sea_Gull_at_Point_Lobos_State_Natural_Reserve%2C_CA.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Seagull_taking_off_the_Sandy_Hook_shore.jpg/240px-Seagull_taking_off_the_Sandy_Hook_shore.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5f/Birdsniper.jpg/270px-Birdsniper.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fe/Kittiwakes.jpg/440px-Kittiwakes.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/71/Larus_marinus_eggs.jpg/440px-Larus_marinus_eggs.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/d/da/Newborn_seagull_03.jpg/440px-Newborn_seagull_03.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/59/Seagull_chicks.jpg/440px-Seagull_chicks.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/74/Glaucous-winged_Gull_RWD1.jpg/350px-Glaucous-winged_Gull_RWD1.jpg", // ktlint-disable max-line-length
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Relict_Gull.jpg/350px-Relict_Gull.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2b/Laughing-gull.jpg/350px-Laughing-gull.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/2/29/Chroicocephalus_ridibundus_%28summer%29.jpg/350px-Chroicocephalus_ridibundus_%28summer%29.jpg" // ktlint-disable max-line-length
        )
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
