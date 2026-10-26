package com.exponea.sdk.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.testutil.testFontFile
import java.io.File
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FontCacheImplTest {

    private val pathToFont = "/path/to/test_font.ttf"
    private val pathToMissingFont = "/path/to/missing_font.ttf"

    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var cache: FontCacheImpl

    @Before
    fun before() {
        Exponea.isStopped = false
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return when (request.requestUrl?.encodedPath) {
                        pathToFont -> MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "font/ttf")
                            .setBody(Buffer().write(testFontFile().readBytes()))
                        else -> MockResponse().setResponseCode(404)
                    }
                }
            }
        }
        server.start()
        File(context.filesDir, FontCacheImpl.DIRECTORY).deleteRecursively()
        cache = FontCacheImpl(context)
    }

    @After
    fun tearDown() {
        Exponea.isStopped = false
        server.shutdown()
        File(context.filesDir, FontCacheImpl.DIRECTORY).deleteRecursively()
    }

    @Test
    fun `should download font and create typeface`() {
        val fontUrl = server.url(pathToFont).toString()

        val typeface = cache.getTypeface(fontUrl)

        assertThat(typeface, notNullValue())
        assertThat(cache.has(fontUrl), equalTo(true))
        assertThat(cache.getFontFile(fontUrl)?.length(), equalTo(testFontFile().length()))
    }

    @Test
    fun `should download font only once`() {
        val fontUrl = server.url(pathToFont).toString()

        assertThat(cache.getTypeface(fontUrl), notNullValue())
        assertThat(cache.getTypeface(fontUrl), notNullValue())

        assertThat(server.requestCount, equalTo(1))
    }

    @Test
    fun `should return null typeface if font is not found`() {
        val fontUrl = server.url(pathToMissingFont).toString()

        assertThat(cache.getTypeface(fontUrl), nullValue())
        assertThat(cache.has(fontUrl), equalTo(false))
    }

    @Test
    fun `should return null typeface if SDK is stopped`() {
        val fontUrl = server.url(pathToFont).toString()
        Exponea.isStopped = true

        assertThat(cache.getTypeface(fontUrl), nullValue())
        assertThat(server.requestCount, equalTo(0))
    }
}
