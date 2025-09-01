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
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleFileCacheTest {

    private val pathToImage = "/path/to/test_image.png"
    private val pathToNonExistingImage = "/path/to/non_existing_image.png"
    private val listOfImagePaths = listOf(
        "/path/to/test_image1.png",
        "/path/to/test_image2.png",
        "/path/to/test_image3.png",
        "/path/to/test_image4.png",
        "/path/to/test_image5.png",
        "/path/to/test_image6.png"
    )

    private lateinit var context: Context
    private lateinit var server: MockWebServer

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return when (request.requestUrl?.encodedPath) {
                        pathToNonExistingImage -> {
                            MockResponse()
                                .setResponseCode(404)
                        }
                        else -> {
                            MockResponse()
                                .setResponseCode(200)
                                .setHeader("Content-Type", "image/png")
                                .setBody("mock-image-response")
                        }
                    }
                }
            }
        }
        server.start()
        File(context.filesDir, DrawableCacheImpl.DIRECTORY).deleteRecursively()
    }

    @After
    fun tearDown() {
        server.shutdown()
        File(context.filesDir, DrawableCacheImpl.DIRECTORY).deleteRecursively()
    }

    @Test
    fun `should download the image by same thread`() {
        val imageUrl = server.url(pathToImage).toString()
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
    fun `should download the image by background thread`() {
        val imageUrl = server.url(pathToImage).toString()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        var downloaded = false
        waitForIt {
            runOnBackgroundThread {
                cache.preload(listOf(imageUrl)) { status ->
                    downloaded = status
                    it()
                }
            }
        }

        assertTrue(cache.has(imageUrl))
        assertTrue(downloaded)
        assertNotNull(cache.getFile(imageUrl))
    }

    @Test
    fun `should download the image only once`() {
        val imageUrl = server.url(pathToImage).toString()
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        val downloadedCount = AtomicInteger(0)
        val downloadAttempts = 1000
        waitForIt {
            repeat(downloadAttempts) {
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
    fun `should download the image only once - 404 test`() {
        val invalidImageUrl = server.url(pathToNonExistingImage).toString()
        val imageUrl = server.url(pathToImage).toString()

        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        val downloadedCount = AtomicInteger(0)
        val downloadAttempts = 1000
        waitForIt {
            repeat(downloadAttempts) { i ->
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
        val invalidImageUrl = server.url(pathToNonExistingImage).toString()
        val validImageUrls = listOfImagePaths.map { server.url(it).toString() }

        val imagesToDownload = validImageUrls.toMutableList().apply {
            add(2, invalidImageUrl)
        }
        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)
        waitForIt { done ->
            cache.preload(imagesToDownload) { status ->
                // failure is OK here
                assertFalse(status)
                done()
            }
        }
        validImageUrls.forEach {
            assertTrue(cache.has(it))
            assertNotNull(cache.getFile(it))
        }
    }

    @Test
    fun `should download multiple images by single thread`() {
        val downloadTimeout = 2_000L // ms
        val responseTimeout = 200L // ms

        val imageUrls = listOfImagePaths.map { server.url(it).toString() }

        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                    in listOfImagePaths -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "image/png")
                            .setBody("mock-response")
                            .setBodyDelay(responseTimeout, TimeUnit.MILLISECONDS)
                    }
                    else -> {
                        MockResponse()
                            .setResponseCode(404)
                    }
                }
            }
        }
        server.dispatcher = dispatcher

        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)

        var downloaded = false
        waitForIt(downloadTimeout * 1000L) {
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

    /**
     * This test verifies concurrent downloading of multiple images using multiple threads.
     *
     * Since the default OkHttp dispatcher allows 5 concurrent requests per host, the dispatcher in SimpleFileCache
     * is configured to ensure all images are downloaded in parallel.
     * Each response is delayed to simulate network latency and confirm all requests complete within the expected time.
     *
     * @see okhttp3.Dispatcher.maxRequestsPerHost
     */
    @Test
    fun `should download multiple images concurrently using multiple threads`() {
        val downloadTimeout = 300L // ms
        val responseTimeout = 200L // ms

        val imageUrls = listOfImagePaths.map { server.url(it).toString() }

        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.requestUrl?.encodedPath) {
                    in listOfImagePaths -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "image/png")
                            .setBody("mock-response")
                            .setBodyDelay(responseTimeout, TimeUnit.MILLISECONDS)
                    }
                    else -> {
                        MockResponse()
                            .setResponseCode(404)
                    }
                }
            }
        }
        server.dispatcher = dispatcher

        assertEquals(6,
            imageUrls.size,
            "Num of requests to process should be greater than default value of Dispatcher.maxRequestsPerHost"
            )

        val cache = SimpleFileCache(context, DrawableCacheImpl.DIRECTORY)

        val downloadedCount = CountDownLatch(imageUrls.size)

        imageUrls.forEach { imageUrl ->
            runOnBackgroundThread {
                cache.preload(listOf(imageUrl)) { status ->
                    downloadedCount.countDown()
                }
            }
        }
        assertTrue(
            downloadedCount.await(downloadTimeout, TimeUnit.MILLISECONDS),
            "Files not downloaded in expected time")
        imageUrls.forEach {
            assertTrue(cache.has(it))
            assertNotNull(cache.getFile(it))
        }
    }
}
