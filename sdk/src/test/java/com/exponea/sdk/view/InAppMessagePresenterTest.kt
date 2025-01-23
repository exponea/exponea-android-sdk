package com.exponea.sdk.view

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.R
import com.exponea.sdk.models.HtmlActionType
import com.exponea.sdk.models.InAppMessage
import com.exponea.sdk.models.InAppMessageButtonType
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessagePayloadButton
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.models.InAppMessageType.ALERT
import com.exponea.sdk.models.InAppMessageType.FREEFORM
import com.exponea.sdk.models.InAppMessageType.FULLSCREEN
import com.exponea.sdk.models.InAppMessageType.MODAL
import com.exponea.sdk.models.InAppMessageType.SLIDE_IN
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.shadows.BadTokenShadowPopupWindow
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.mocks.MockApplication
import com.exponea.sdk.testutil.waitForIt
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.HtmlNormalizer.NormalizedResult
import com.google.android.material.behavior.SwipeDismissBehavior
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
internal class InAppMessagePresenterTest(
    val inAppMessageType: InAppMessageType
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): List<Array<out Any?>> {
            return InAppMessageType.values()
                    .map { arrayOf(it) }
        }
    }

    private val emptyDismiss: (Activity, Boolean, InAppMessagePayloadButton?) -> Unit = { _, _, _ -> }
    lateinit var context: Context
    lateinit var server: MockWebServer
    var payload: InAppMessagePayload? = null
    private var payloadHtml: NormalizedResult? = null
    private lateinit var inAppMessage: InAppMessage

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = ExponeaMockServer.createServer()
        File(context.cacheDir, InAppMessageBitmapCacheImpl.DIRECTORY).deleteRecursively()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val fontCache = FontCacheImpl(context)
        inAppMessage = InAppMessageTest.buildInAppMessage(
            environment = server,
            type = inAppMessageType
        )
        payload = inAppMessage.payload
        payloadHtml = inAppMessage.payloadHtml?.let { HtmlNormalizer(bitmapCache, fontCache, it).normalize() }
    }

    @Test
    fun `should not show in-app without resumed activity`() {
        buildActivity(AppCompatActivity::class.java).setup()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                ApplicationProvider.getApplicationContext(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        assertNull(
            presenter.show(inAppMessageType, payload, payloadHtml, null, mockk(), emptyDismiss, {})
        )
    }

    @Test
    fun `should show in-app only once and activity is resumed`() {
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                ApplicationProvider.getApplicationContext(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        buildActivity(AppCompatActivity::class.java).setup().resume()
        assertNotNull(
            presenter.show(inAppMessageType, payload, payloadHtml, null, mockk(), emptyDismiss, {})
        )
    }

    @Test
    @Config(shadows = [BadTokenShadowPopupWindow::class])
    fun `should not show in-app if activity is destroyed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val presenter = InAppMessagePresenter(context, bitmapCache)
        preloadMessageImages(bitmapCache)
        var catchedError: String? = null
        val activity = buildActivity(BadTokenActivity::class.java).setup().get()
        val view = presenter.getView(
            activity,
            inAppMessageType,
            payload,
            payloadHtml,
            null,
            mockk(),
            mockk(),
            { catchedError = it }
        )
        assertNotNull(view)
        activity.badTokenActive = true
        view.show()
        assertNotNull(catchedError)
    }

    @Test
    @Config(shadows = [BadTokenShadowPopupWindow::class])
    fun `should not crash while dismiss in-app if activity is destroyed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bitmapCache = InAppMessageBitmapCacheImpl(context)
        val presenter = InAppMessagePresenter(context, bitmapCache)
        preloadMessageImages(bitmapCache)
        val activity = buildActivity(BadTokenActivity::class.java).setup().get()
        val view = presenter.getView(
            activity,
            inAppMessageType,
            payload,
            payloadHtml,
            null,
            { },
            { _, _ ->
                if (inAppMessageType == FREEFORM) {
                    // HTML WebView has no decorView in Robolectric, we have to simulate it
                    throw WindowManager.BadTokenException("Activity is closed and has no active window token")
                }
                if (inAppMessageType == SLIDE_IN) {
                    // SlideIn has no decorView in Robolectric, we have to simulate it
                    throw WindowManager.BadTokenException("Activity is closed and has no active window token")
                }
            },
            { }
        )
        assertNotNull(view)
        view.show()
        assertTrue(view.isPresented)
        activity.badTokenActive = true
        view.dismiss()
        if (inAppMessageType == SLIDE_IN) {
            // wait for SlideIn animation end
            shadowOf(Looper.getMainLooper()).idle()
        }
        // no error tracked, no exception thrown
    }

    class BadTokenActivity : Activity() {
        var badTokenActive: Boolean = false
        companion object {
            val robolectricActivity = buildActivity(AppCompatActivity::class.java).setup().get()
        }
        override fun getSystemService(name: String): Any? {
            return when (name) {
                Context.WINDOW_SERVICE -> {
                    val windowManager = spyk(robolectricActivity.getSystemService(name)) as WindowManager
                    every { windowManager.addView(any(), any()) } answers {
                        if (badTokenActive) {
                            throw WindowManager.BadTokenException("Activity is closed and has no active window token")
                        } else {
                            callOriginal()
                        }
                    }
                    every { windowManager.removeViewImmediate(any()) } answers {
                        if (badTokenActive) {
                            throw WindowManager.BadTokenException("Activity is closed and has no active window token")
                        } else {
                            callOriginal()
                        }
                    }
                    windowManager
                }
                else -> robolectricActivity.getSystemService(name)
            }
        }
    }

    @Test
    fun `should show in-app when initialized with resumed activity`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                activity.get(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        assertNotNull(
            presenter.show(inAppMessageType, payload, payloadHtml, null, mockk(), emptyDismiss, {})
        )
    }

    @Test
    fun `should not show in-app after activity is paused`() {
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                ApplicationProvider.getApplicationContext(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented =
            presenter.show(inAppMessageType, payload, payloadHtml, null, mockk(), emptyDismiss, {})
        assertNotNull(presented)
        presented.dismissedCallback(false, null)
        buildActivity(AppCompatActivity::class.java).setup().resume().pause()
        assertNull(presenter.show(inAppMessageType, payload, payloadHtml, null, mockk(), emptyDismiss, {}))
    }

    @Test
    fun `should only show one in-app at a time`() {
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                ApplicationProvider.getApplicationContext(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented =
            presenter.show(inAppMessageType, payload, payloadHtml, null, mockk(), emptyDismiss, {})
        assertNotNull(presented)
        assertNull(presenter.show(inAppMessageType, payload, payloadHtml, null, mockk(), emptyDismiss, {}))
        presented.dismissedCallback(false, null)
        Robolectric.flushForegroundThreadScheduler() // skip animations
        assertNotNull(
            presenter.show(inAppMessageType, payload, payloadHtml, null, mockk(), emptyDismiss, {})
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should dismiss the in-app after timeout has passed`() {
        if (inAppMessageType == SLIDE_IN) {
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
        var dismissActivity: Activity? = null
        var dismissInteraction: Boolean? = null
        var dismissButton: InAppMessagePayloadButton? = null
        val emptyAction: (Activity, InAppMessagePayloadButton) -> Unit = { _, _ -> }
        val presented =
            presenter.show(inAppMessageType, payload, payloadHtml, 1234, emptyAction, { a, b, c ->
                dismissActivity = a
                dismissInteraction = b
                dismissButton = c
            }, {})

        mockkObject(Exponea)
        every { Exponea.presentedInAppMessage } returns presented
        every { Exponea.inAppMessagePresenter } returns presenter
        val intent = shadowOf(activity.get()).peekNextStartedActivity()
        buildActivity(InAppMessageActivity::class.java, intent).create().resume()
        Robolectric.getForegroundThreadScheduler().advanceBy(1233, TimeUnit.MILLISECONDS)
        assertNull(dismissActivity)
        assertNull(dismissInteraction)
        assertNull(dismissButton)
        Robolectric.getForegroundThreadScheduler().advanceBy(1, TimeUnit.HOURS)
        assertEquals(activity.get(), dismissActivity)
        assertEquals(false, dismissInteraction)
        assertNull(dismissButton)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should dismiss the in-app silently after timeout has passed and activity is already destroyed`() {
        if (inAppMessageType == SLIDE_IN) {
            // we'll skip this for slide-in that requires rendering of the dialog itself
            return
        }
        val triggerActivity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
                triggerActivity.get(),
                bitmapCache
        )
        preloadMessageImages(bitmapCache)
        var dismissActivity: Activity? = null
        var dismissInteraction: Boolean? = null
        var dismissButton: InAppMessagePayloadButton? = null
        val emptyAction: (Activity, InAppMessagePayloadButton) -> Unit = { _, _ -> }
        val presented =
            presenter.show(inAppMessageType, payload, payloadHtml, 1234, emptyAction, { a, b, c ->
                dismissActivity = a
                dismissInteraction = b
                dismissButton = c
            }, {})
        mockkObject(Exponea)
        every { Exponea.presentedInAppMessage } returns presented
        every { Exponea.inAppMessagePresenter } returns presenter
        val intent = shadowOf(triggerActivity.get()).peekNextStartedActivity()
        val inAppActivity = buildActivity(InAppMessageActivity::class.java, intent).create().resume()
        Robolectric.getForegroundThreadScheduler().advanceBy(233, TimeUnit.MILLISECONDS)
        assertNull(dismissActivity)
        assertNull(dismissInteraction)
        assertNull(dismissButton)
        // simulate activity being destroyed without dismissing in-app
        inAppActivity.get().presentedMessageView = null
        inAppActivity.pause().stop().destroy()
        triggerActivity.pause().stop().destroy()
        Robolectric.getForegroundThreadScheduler().advanceBy(1000, TimeUnit.MILLISECONDS)
        assertNull(dismissActivity)
        assertNull(dismissInteraction)
        assertNull(dismissButton)
        Robolectric.getForegroundThreadScheduler().advanceBy(1, TimeUnit.HOURS)
        assertEquals(triggerActivity.get(), dismissActivity)
        assertEquals(false, dismissInteraction)
        assertNull(dismissButton)
    }

    private fun preloadMessageImages(bitmapCache: InAppMessageBitmapCacheImpl) {
        payload?.imageUrl?.let { imageUrl ->
            server.enqueue(MockResponse().setBody("mock-response"))
            waitForIt { bitmapCache.preload(listOf(imageUrl)) { it() } }
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call actionCallback for real click`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        preloadMessageImages(bitmapCache)
        var catchedActionButton: InAppMessagePayloadButton? = null
        var catchedClosedByUser: Boolean? = null
        var catchedError: String? = null
        var catchedCancelButton: InAppMessagePayloadButton? = null
        val view = presenter.getView(
            activity.get(),
            inAppMessageType,
            payload,
            payloadHtml,
            inAppMessage.timeout,
            { actionButton ->
                // should be called only once
                assertNull(catchedActionButton)
                catchedActionButton = actionButton
            },
            { closedByUser, cancelButton ->
                // should be called only once
                assertNull(catchedClosedByUser)
                catchedClosedByUser = closedByUser
                catchedCancelButton = cancelButton
            },
            { errorMessage ->
                // should be called only once
                assertNull(catchedError)
                catchedError = errorMessage
            }
        )
        assertNotNull(view)
        view.show()
        // simulate action click
        when (inAppMessageType) {
            MODAL, FULLSCREEN -> {
                val realView = view as InAppMessageDialog
                realView.findViewById<Button>(R.id.buttonAction1).performClick()
            }
            ALERT -> {
                val realView = view as InAppMessageAlert
                realView.dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            }
            SLIDE_IN -> {
                val realView = view as InAppMessageSlideIn
                realView.contentView.findViewById<Button>(R.id.buttonAction1).performClick()
            }
            FREEFORM -> {
                val realView = view as InAppMessageWebview
                realView.handleActionClick("https://someaddress.com")
            }
        }
        // validate callbacks
        assertNotNull(catchedActionButton)
        assertNull(catchedClosedByUser)
        assertNull(catchedCancelButton)
        assertNull(catchedError)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call dismissedCallback for close from user`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        preloadMessageImages(bitmapCache)
        var catchedActionButton: InAppMessagePayloadButton? = null
        var catchedClosedByUser: Boolean? = null
        var catchedError: String? = null
        var catchedCancelButton: InAppMessagePayloadButton? = null
        val view = presenter.getView(
            activity.get(),
            inAppMessageType,
            payload,
            payloadHtml,
            inAppMessage.timeout,
            { actionButton ->
                // should be called only once
                assertNull(catchedActionButton)
                catchedActionButton = actionButton
            },
            { closedByUser, cancelButton ->
                // should be called only once
                assertNull(catchedClosedByUser)
                catchedClosedByUser = closedByUser
                catchedCancelButton = cancelButton
            },
            { errorMessage ->
                // should be called only once
                assertNull(catchedError)
                catchedError = errorMessage
            }
        )
        assertNotNull(view)
        view.show()
        // simulate action click
        when (inAppMessageType) {
            MODAL, FULLSCREEN -> {
                val realView = view as InAppMessageDialog
                realView.findViewById<Button>(R.id.buttonClose).performClick()
            }
            ALERT -> {
                val realView = view as InAppMessageAlert
                realView.dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
            }
            SLIDE_IN -> {
                val realView = view as InAppMessageSlideIn
                realView.contentView.findViewById<Button>(R.id.buttonAction2).performClick()
            }
            FREEFORM -> {
                val realView = view as InAppMessageWebview
                val closeAction = payloadHtml?.actions?.find { it.actionType == HtmlActionType.CLOSE }
                assertNotNull(closeAction)
                realView.handleActionClick(closeAction.actionUrl)
            }
        }
        // validate callbacks
        assertNull(catchedActionButton)
        assertNotNull(catchedClosedByUser)
        assertTrue(catchedClosedByUser!!)
        when (inAppMessageType) {
            MODAL, FULLSCREEN -> {
                // X button doesn't deliver button info
                assertNull(catchedCancelButton)
            }
            FREEFORM -> {
                assertNotNull(catchedCancelButton)
                assertEquals(
                    payloadHtml?.actions?.find { it.actionType == HtmlActionType.CLOSE }?.buttonText,
                    catchedCancelButton!!.buttonText
                )
            }
            ALERT, SLIDE_IN -> {
                assertNotNull(catchedCancelButton)
                assertEquals(
                    payload?.buttons?.first { it.buttonType == InAppMessageButtonType.CANCEL }?.buttonText,
                    catchedCancelButton!!.buttonText
                )
            }
        }
        assertNull(catchedError)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call dismissedCallback for swipe-close from user`() {
        if (inAppMessageType != SLIDE_IN) {
            return
        }
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        preloadMessageImages(bitmapCache)
        var catchedActionButton: InAppMessagePayloadButton? = null
        var catchedClosedByUser: Boolean? = null
        var catchedError: String? = null
        var catchedCancelButton: InAppMessagePayloadButton? = null
        val view = presenter.getView(
            activity.get(),
            inAppMessageType,
            payload,
            payloadHtml,
            inAppMessage.timeout,
            { actionButton ->
                // should be called only once
                assertNull(catchedActionButton)
                catchedActionButton = actionButton
            },
            { closedByUser, cancelButton ->
                // should be called only once
                assertNull(catchedClosedByUser)
                catchedClosedByUser = closedByUser
                catchedCancelButton = cancelButton
            },
            { errorMessage ->
                // should be called only once
                assertNull(catchedError)
                catchedError = errorMessage
            }
        )
        assertNotNull(view)
        view.show()
        // simulate swipe-right close
        val realView = view as InAppMessageSlideIn
        val containerView = realView.contentView.findViewById<LinearLayout>(R.id.inAppMessageSlideInContainer)
        val behavior = (containerView.layoutParams as CoordinatorLayout.LayoutParams).behavior as SwipeDismissBehavior
        behavior.listener!!.onDismiss(realView.contentView)
        // validate callbacks
        assertNull(catchedActionButton)
        assertNotNull(catchedClosedByUser)
        assertTrue(catchedClosedByUser!!)
        assertNull(catchedCancelButton)
        assertNull(catchedError)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.PAUSED)
    fun `should call dismissedCallback for close for timeout`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext())
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        preloadMessageImages(bitmapCache)
        var catchedActionButton: InAppMessagePayloadButton? = null
        var catchedClosedByUser: Boolean? = null
        var catchedError: String? = null
        var catchedCancelButton: InAppMessagePayloadButton? = null
        val view = presenter.getView(
            activity.get(),
            inAppMessageType,
            payload,
            payloadHtml,
            5000,
            { actionButton ->
                // should be called only once
                assertNull(catchedActionButton)
                catchedActionButton = actionButton
            },
            { closedByUser, cancelButton ->
                // should be called only once
                assertNull(catchedClosedByUser)
                catchedClosedByUser = closedByUser
                catchedCancelButton = cancelButton
            },
            { errorMessage ->
                // should be called only once
                assertNull(catchedError)
                catchedError = errorMessage
            }
        )
        assertNotNull(view)
        view.show()
        // wait for timeout
        Robolectric.getForegroundThreadScheduler().advanceBy(5100, TimeUnit.MILLISECONDS)
        Robolectric.flushForegroundThreadScheduler()
        // validate callbacks
        assertNull(catchedActionButton)
        assertNotNull(catchedClosedByUser)
        assertFalse(catchedClosedByUser!!)
        assertNull(catchedCancelButton)
        assertNull(catchedError)
    }
}
