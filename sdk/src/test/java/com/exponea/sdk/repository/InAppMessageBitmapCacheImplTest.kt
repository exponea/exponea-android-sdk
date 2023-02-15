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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBitmapFactory

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
        assertNull(repo.get("http://domain.com/image.jpg"))
        assertFalse(repo.has("http://domain.com/image.jpg"))
    }

    @Test
    fun `should preload image`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val imageUrl = server.url("image.jpg").toString()

        val repo = InAppMessageBitmapCacheImpl(context)
        waitForIt { repo.preload(listOf(imageUrl)) { it() } }
        assertNotNull(repo.get(imageUrl))
    }

    @Test
    fun `should download image with same url only once`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        // real image
        val imageUrl = server.url(
            "https://storage.googleapis.com/exp-app-storage/" +
                    "f02807dc-6b57-11e9-8cc8-0a580a203636/media/original/" +
                    "fda24b2c-5ccf-11ec-9e7d-224548c7f76e"
        ).toString()

        val repo = spyk(InAppMessageBitmapCacheImpl(context))
        verify(exactly = 0) { repo.downloadImage(any(), any()) }
        waitForIt { repo.preload(listOf(imageUrl)) { it() } }
        verify(exactly = 1) { repo.downloadImage(any(), any()) }
        waitForIt { repo.preload(listOf(imageUrl)) { it() } }
        verify(exactly = 1) { repo.downloadImage(any(), any()) }
    }

    @Test
    fun `should download image with different url`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        // real image
        val imageUrl1 = server.url(
            "https://storage.googleapis.com/exp-app-storage/" +
                    "f02807dc-6b57-11e9-8cc8-0a580a203636/media/original/" +
                    "fda24b2c-5ccf-11ec-9e7d-224548c7f76e"
        ).toString()

        val imageUrl2 = server.url(
            "https://storage.googleapis.com/exp-app-storage/" +
                    "f02807dc-6b57-11e9-8cc8-0a580a203636/media/original/" +
                    "a20fdf92-5cd2-11ec-819f-a64145d9ff9e"
        ).toString()

        val repo = spyk(InAppMessageBitmapCacheImpl(context))
        verify(exactly = 0) { repo.downloadImage(any(), any()) }
        waitForIt { repo.preload(listOf(imageUrl1)) { it() } }
        verify(exactly = 1) { repo.downloadImage(any(), any()) }
        waitForIt { repo.preload(listOf(imageUrl2)) { it() } }
        verify(exactly = 2) { repo.downloadImage(any(), any()) }
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

    @Test @Config(qualifiers = "w200dp-h300dp-xhdpi") // screen size is 400x600
    fun `should downsample image`() {
        server.enqueue(MockResponse().setBody("mock-response"))
        val imageUrl = server.url("image.jpg").toString()

        val repo = InAppMessageBitmapCacheImpl(context)
        waitForIt { repo.preload(listOf(imageUrl)) { it() } }

        val storedFilePath = File(
            File(context.cacheDir, InAppMessageBitmapCacheImpl.DIRECTORY),
            repo.getFileName(imageUrl)
        ).absolutePath

        ShadowBitmapFactory.provideWidthAndHeightHints(storedFilePath, 800, 600)
        assertEquals(800, repo.get(imageUrl)?.width)
        assertEquals(600, repo.get(imageUrl)?.height)

        ShadowBitmapFactory.provideWidthAndHeightHints(storedFilePath, 1200, 600)
        assertEquals(600, repo.get(imageUrl)?.width)
        assertEquals(300, repo.get(imageUrl)?.height)

        ShadowBitmapFactory.provideWidthAndHeightHints(storedFilePath, 2000, 1200)
        assertEquals(666, repo.get(imageUrl)?.width)
        assertEquals(400, repo.get(imageUrl)?.height)
    }
}
