package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.backgroundThreadDispatcher
import com.exponea.sdk.util.mainThreadDispatcher
import io.mockk.spyk
import io.mockk.verify
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.Call
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageBitmapCacheImplTest {
    lateinit var context: Context
    lateinit var server: MockWebServer
    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = ExponeaMockServer.createServer()
        File(context.cacheDir, InAppMessageBitmapCacheImpl.DIRECTORY).deleteRecursively()
    }

    @Before
    fun overrideThreadBehaviour() {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Main)
    }

    @After
    fun restoreThreadBehaviour() {
        mainThreadDispatcher = CoroutineScope(Dispatchers.Main)
        backgroundThreadDispatcher = CoroutineScope(Dispatchers.Default)
    }

    @Test
    fun `should get nil on cold start`() {
        val repo = InAppMessageBitmapCacheImpl(context)
        assertNull(repo.getFile("http://domain.com/image.jpg"))
        assertFalse(repo.has("http://domain.com/image.jpg"))
    }

    @Test
    fun `should preload image`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val imageUrl = server.url("image.jpg").toString()

        val repo = InAppMessageBitmapCacheImpl(context)
        waitForIt { repo.preload(listOf(imageUrl)) { it() } }
        assertNotNull(repo.getFile(imageUrl))
    }

    @Test
    fun `should download image with same url only once`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        // real image
        val imageUrl = server.url(
            "/exp-app-storage/" +
                    "f02807dc-6b57-11e9-8cc8-0a580a203636/media/original/" +
                    "fda24b2c-5ccf-11ec-9e7d-224548c7f76e"
        ).toString()

        val repo = spyk(InAppMessageBitmapCacheImpl(context))
        verify(exactly = 0) { repo.downloadFile(any(), any()) }
        waitForIt { repo.preload(listOf(imageUrl)) { it() } }
        verify(exactly = 1) { repo.downloadFile(any(), any()) }
        waitForIt { repo.preload(listOf(imageUrl)) { it() } }
        verify(exactly = 1) { repo.downloadFile(any(), any()) }
    }

    @Test
    fun `should download image with different url`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        // real image
        val imageUrl1 = server.url(
            "/exp-app-storage/" +
                    "f02807dc-6b57-11e9-8cc8-0a580a203636/media/original/" +
                    "fda24b2c-5ccf-11ec-9e7d-224548c7f76e"
        ).toString()

        val imageUrl2 = server.url(
            "/exp-app-storage/" +
                    "f02807dc-6b57-11e9-8cc8-0a580a203636/media/original/" +
                    "a20fdf92-5cd2-11ec-819f-a64145d9ff9e"
        ).toString()

        val repo = spyk(InAppMessageBitmapCacheImpl(context))
        verify(exactly = 0) { repo.downloadFile(any(), any()) }
        waitForIt { repo.preload(listOf(imageUrl1)) { it() } }
        verify(exactly = 1) { repo.downloadFile(any(), any()) }
        waitForIt { repo.preload(listOf(imageUrl2)) { it() } }
        verify(exactly = 2) { repo.downloadFile(any(), any()) }
    }

    @Test
    fun `should clear cached images`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        server.enqueue(MockResponse().setBody("mock-response"))
        server.enqueue(MockResponse().setBody("mock-response"))
        val image1Url = server.url("image1.jpg").toString()
        val image2Url = server.url("image2.jpg").toString()
        val image3Url = server.url("image3.jpg").toString()

        val repo = InAppMessageBitmapCacheImpl(context)
        waitForIt { repo.preload(listOf(image1Url)) { it() } }
        waitForIt { repo.preload(listOf(image2Url)) { it() } }
        waitForIt { repo.preload(listOf(image3Url)) { it() } }
        assertTrue(repo.has(image1Url))
        assertTrue(repo.has(image2Url))
        assertTrue(repo.has(image3Url))

        repo.clearExcept(arrayListOf(image2Url))
        assertFalse(repo.has(image1Url))
        assertTrue(repo.has(image2Url))
        assertFalse(repo.has(image3Url))
    }

    @Test
    fun `should download and store image with URL length up to 2000 characters`() {
        val repo = spyk(InAppMessageBitmapCacheImpl(context))
        val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        for (nameLength in 1..2000) {
            server.enqueue(MockResponse().setBody("mock-response"))
            val imageUrl = server.url(
                List(nameLength) { alphabet.random() }.joinToString("")
            ).toString()
            waitForIt { repo.downloadFile(imageUrl) { it() } }
            assertTrue(repo.has(imageUrl), "Failure for length $nameLength")
        }
    }

    @Test
    fun `should download image with long URL - from prod`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val imageUrl = server.url(
            """
            /img/media/eadded8b1adc2e808cb7ddaf2c09dcb0f5b3126b/0_1720_2333_1399/master/2333.jpg?width=1200&height=1200&quality=85&auto=format&fit=crop&s=b0988ec9b039a42a1c70f7b3b9956d49
            """.trimIndent()
        ).toString()
        val repo = spyk(InAppMessageBitmapCacheImpl(context))
        repo.getFileName(imageUrl).length
        waitForIt { repo.preload(listOf(imageUrl)) { it() } }
        verify(exactly = 1) { repo.downloadFile(any(), any()) }
        assertTrue(repo.has(imageUrl))
    }

    @Test
    fun `should not download image with non http url`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        // real image
        val imageUrl = "noscheme://example.com/image.jpg"
        val repo = InAppMessageBitmapCacheImpl(context)
        var downloaded = true
        var downloadCall: Call? = null
        waitForIt { done ->
            downloadCall = repo.downloadFile(imageUrl) { downloadStatus ->
                downloaded = downloadStatus
                done()
            }
        }
        assertFalse(downloaded)
        assertNull(downloadCall)
    }
}
