package com.exponea.sdk.view

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.mocks.MockApplication
import com.exponea.sdk.testutil.waitForIt
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Robolectric.buildActivity
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = MockApplication::class)
internal class InAppMessageDialogPresenterTest(
    val inAppMessageType: InAppMessageType
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): List<Array<out Any?>> {
            return InAppMessageType.values()
                    .filter { it != InAppMessageType.FREEFORM }
                    .map { arrayOf(it) }
        }
    }

    lateinit var context: Context
    lateinit var server: MockWebServer
    var payload: InAppMessagePayload? = null

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = ExponeaMockServer.createServer()
        File(context.cacheDir, InAppMessageBitmapCacheImpl.DIRECTORY).deleteRecursively()
        payload = InAppMessageTest.getInAppMessage(environment = server).payload
    }

    @Test
    fun `should not show dialog without resumed activity`() {
        buildActivity(AppCompatActivity::class.java).setup()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                ApplicationProvider.getApplicationContext(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        assertNull(presenter.show(inAppMessageType, payload, null, null, mockk(), {}, {}))
    }

    @Test
    fun `should show dialog once and activity is resumed`() {
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                ApplicationProvider.getApplicationContext(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        buildActivity(AppCompatActivity::class.java).setup().resume()
        assertNotNull(
            presenter.show(inAppMessageType, payload, null, null, mockk(), {}, {})
        )
    }

    @Test
    fun `should show dialog when initialized with resumed activity`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                activity.get(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        assertNotNull(
            presenter.show(inAppMessageType, payload, null, null, mockk(), {}, {})
        )
    }

    @Test
    fun `should not show dialog after activity is paused`() {
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                ApplicationProvider.getApplicationContext(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented =
            presenter.show(inAppMessageType, payload, null, null, mockk(), {}, {})
        assertNotNull(presented)
        presented.dismissedCallback()
        buildActivity(AppCompatActivity::class.java).setup().resume().pause()
        assertNull(presenter.show(inAppMessageType, payload, null, null, mockk(), {}, {}))
    }

    @Test
    fun `should only show one dialog at a time`() {
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                ApplicationProvider.getApplicationContext(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented =
            presenter.show(inAppMessageType, payload, null, null, mockk(), {}, {})
        assertNotNull(presented)
        assertNull(presenter.show(inAppMessageType, payload, null, null, mockk(), {}, {}))
        presented.dismissedCallback()
        Robolectric.flushForegroundThreadScheduler() // skip animations
        assertNotNull(
            presenter.show(inAppMessageType, payload, null, null, mockk(), {}, {})
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should dismiss the dialog after timeout has passed`() {
        if (inAppMessageType == InAppMessageType.SLIDE_IN) {
            // we'll skip this for slide-in that requires rendering of the dialog itself
            return
        }
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                activity.get(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        val dismiss = spyk<(Activity) -> Unit>()
        val presented =
            presenter.show(inAppMessageType, payload, null, 1234, mockk(), dismiss, {})

        mockkObject(Exponea)
        every { Exponea.presentedInAppMessage } returns presented
        every { Exponea.inAppMessagePresenter } returns presenter
        val intent = shadowOf(activity.get()).peekNextStartedActivity()
        buildActivity(InAppMessageActivity::class.java, intent).create().resume()

        Robolectric.getForegroundThreadScheduler().advanceBy(1233, TimeUnit.MILLISECONDS)
        verify(exactly = 0) { dismiss(activity.get()) }
        Robolectric.getForegroundThreadScheduler().advanceBy(1, TimeUnit.HOURS)
        verify(exactly = 1) { dismiss(activity.get()) }
    }

    private fun preloadMessageImages(bitmapCache: InAppMessageBitmapCacheImpl) {
        payload?.imageUrl?.let { imageUrl ->
            server.enqueue(MockResponse().setBody("mock-response"))
            waitForIt { bitmapCache.preload(listOf(imageUrl)) { it() } }
        }
    }
}
