package com.exponea.sdk.view

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Build
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.cardview.widget.CardView
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
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.FontCache
import com.exponea.sdk.shadows.BadTokenShadowPopupWindow
import com.exponea.sdk.style.InAppRichstylePayloadBuilder
import com.exponea.sdk.testutil.ExponeaMockServer
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.testutil.mocks.MockApplication
import com.exponea.sdk.util.HtmlNormalizer
import com.exponea.sdk.util.HtmlNormalizer.NormalizedResult
import com.exponea.sdk.view.layout.RowFlexLayout
import com.google.android.material.behavior.SwipeDismissBehavior
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
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
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
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
    private lateinit var bitmapCache: DrawableCache
    private lateinit var fontCache: FontCache

    @Before
    fun mockCaches() {
        bitmapCache = mockk()
        every { bitmapCache.preload(any(), any()) } answers {
            arg<(Boolean) -> Unit>(1).invoke(true)
        }
        every { bitmapCache.clear() } just Runs
        every { bitmapCache.has(any()) } returns true
        every { bitmapCache.getFile(any()) } returns MockFile()
        every { bitmapCache.getDrawable(any<Int>()) } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every {
            bitmapCache.getDrawable(any<String>())
        } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every { bitmapCache.showImage(any(), any(), any()) } just Runs
        fontCache = mockk()
        every { fontCache.has(any()) } returns true
        every { fontCache.clear() } just Runs
        every { fontCache.getFontFile(any()) } returns File(
            this.javaClass.classLoader!!.getResource("xtrusion.ttf")!!.file
        )
        every { fontCache.getTypeface(any()) } returns Typeface.createFromFile(
            this.javaClass.classLoader!!.getResource("xtrusion.ttf")!!.file
        )
        every { fontCache.preload(any(), any()) } answers {
            arg<(Boolean) -> Unit>(1).invoke(true)
        }
    }

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = ExponeaMockServer.createServer()
        File(context.cacheDir, DrawableCacheImpl.DIRECTORY).deleteRecursively()
        inAppMessage = InAppMessageTest.buildInAppMessageWithRichstyle(
            environment = server,
            type = inAppMessageType
        )
        payload = inAppMessage.payload
        payloadHtml = inAppMessage.payloadHtml?.let { HtmlNormalizer(bitmapCache, fontCache, it).normalize() }
    }

    @After
    fun reset() {
        Exponea.isStopped = false
    }

    @Test
    fun `should not show in-app without resumed activity`() {
        buildActivity(AppCompatActivity::class.java).setup()
        val presenter = InAppMessagePresenter(
            ApplicationProvider.getApplicationContext(),
            bitmapCache
        )
        assertNull(presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {}))
    }

    @Test
    fun `should show dialog once and activity is resumed`() {
        val presenter = InAppMessagePresenter(
            ApplicationProvider.getApplicationContext(),
            bitmapCache
        )
        buildActivity(AppCompatActivity::class.java).setup().resume()
        assertNotNull(
            presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {})
        )
    }

    @Test
    @Config(shadows = [BadTokenShadowPopupWindow::class])
    fun `should not show in-app if activity is destroyed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val presenter = InAppMessagePresenter(context, bitmapCache)
        var catchedError: String? = null
        val activity = buildActivity(BadTokenActivity::class.java).setup().get()
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(bitmapCache, fontCache)
        val view = presenter.getView(
            activity,
            inAppMessageType,
            payload,
            payload?.let { uiPayloadBuilder.build(it) },
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
        val presenter = InAppMessagePresenter(context, bitmapCache)
        val activity = buildActivity(BadTokenActivity::class.java).setup().get()
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(bitmapCache, fontCache)
        val view = presenter.getView(
            activity,
            inAppMessageType,
            payload,
            payload?.let { uiPayloadBuilder.build(it) },
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
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        assertNotNull(
            presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {})
        )
    }

    @Test
    fun `should not show in-app with resumed activity but SDK is stopping`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        Exponea.isStopped = true
        assertNull(
            presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {})
        )
    }

    @Test
    fun `should not show in-app after activity is paused`() {
        val presenter = InAppMessagePresenter(
            ApplicationProvider.getApplicationContext(),
            bitmapCache
        )
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented =
            presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {})
        assertNotNull(presented)
        presented.dismissedCallback(false, null)
        buildActivity(AppCompatActivity::class.java).setup().resume().pause()
        assertNull(presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {}))
    }

    @Test
    fun `should only show one in-app at a time`() {
        val presenter = InAppMessagePresenter(
            ApplicationProvider.getApplicationContext(),
            bitmapCache
        )
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented =
            presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {})
        assertNotNull(presented)
        assertNull(presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {}))
        presented.dismissedCallback(false, null)
        Robolectric.flushForegroundThreadScheduler() // skip animations
        assertNotNull(
            presenter.show(inAppMessageType, payload, null, payloadHtml, null, mockk(), emptyDismiss, {})
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
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        var dismissActivity: Activity? = null
        var dismissInteraction: Boolean? = null
        var dismissButton: InAppMessagePayloadButton? = null
        val emptyAction: (Activity, InAppMessagePayloadButton) -> Unit = { _, _ -> }
        val presented =
            presenter.show(inAppMessageType, payload, null, payloadHtml, 1234, emptyAction, { a, b, c ->
                dismissActivity = a
                dismissInteraction = b
                dismissButton = c
            }, {})
        mockkObject(Exponea)
        every { Exponea.presentedInAppMessage } returns presented
        every { Exponea.inAppMessagePresenter } returns presenter
        val intent = shadowOf(activity.get()).peekNextStartedActivity()
        val controller = buildActivity(InAppMessageActivity::class.java, intent).create()
        controller.start()
        controller.postCreate(null)
        try {
            controller.resume()
        } catch (e: Exception) {
            // Robolectric is shadowing ViewGroup but removeView doesn't work
            // As Buttons are moving to new parents, removeView is required to work
        }
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
    fun `should dismiss the in-app if SDK is stopping`() {
        if (inAppMessageType == SLIDE_IN) {
            // we'll skip this for slide-in that requires rendering of the dialog itself
            return
        }
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        var dismissActivity: Activity? = null
        var dismissInteraction: Boolean? = null
        var dismissButton: InAppMessagePayloadButton? = null
        val emptyAction: (Activity, InAppMessagePayloadButton) -> Unit = { _, _ -> }
        val presented =
            presenter.show(inAppMessageType, payload, null, payloadHtml, null, emptyAction, { a, b, c ->
                dismissActivity = a
                dismissInteraction = b
                dismissButton = c
            }, {})
        assertNotNull(presented)
        mockkObject(Exponea)
        every { Exponea.presentedInAppMessage } returns presented
        every { Exponea.inAppMessagePresenter } returns presenter
        val intent = shadowOf(activity.get()).peekNextStartedActivity()
        val controller = buildActivity(InAppMessageActivity::class.java, intent).create()
        controller.start()
        controller.postCreate(null)
        try {
            controller.resume()
        } catch (e: Exception) {
            // Robolectric is shadowing ViewGroup but removeView doesn't work
            // As Buttons are moving to new parents, removeView is required to work
        }
        Exponea.isStopped = true
        Exponea.deintegration.notifyDeintegration()
        assertNull(dismissActivity)
        assertNull(dismissInteraction)
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
        val presenter = InAppMessagePresenter(
                triggerActivity.get(),
                bitmapCache
        )
        var dismissActivity: Activity? = null
        var dismissInteraction: Boolean? = null
        var dismissButton: InAppMessagePayloadButton? = null
        val emptyAction: (Activity, InAppMessagePayloadButton) -> Unit = { _, _ -> }
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(bitmapCache, fontCache)
        val presented =
            presenter.show(
                inAppMessageType,
                payload,
                null,
                payloadHtml,
                1234,
                emptyAction,
                { a, b, c ->
                    dismissActivity = a
                    dismissInteraction = b
                    dismissButton = c
                },
                {})
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

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @LooperMode(LooperMode.Mode.LEGACY)
    fun `should call actionCallback for real click`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        var catchedActionButton: InAppMessagePayloadButton? = null
        var catchedClosedByUser: Boolean? = null
        var catchedError: String? = null
        var catchedCancelButton: InAppMessagePayloadButton? = null
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(bitmapCache, fontCache)
        val view = presenter.getView(
            activity.get(),
            inAppMessageType,
            payload,
            payload?.let { uiPayloadBuilder.build(it) },
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
                val realView = view as InAppMessageRichstyleDialog
                realView.findViewById<RowFlexLayout>(R.id.buttonsContainer).realChildren.first().performClick()
            }
            ALERT -> {
                val realView = view as InAppMessageAlert
                realView.dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
            }
            SLIDE_IN -> {
                val realView = view as InAppMessageRichstyleSlideIn
                realView.contentView.findViewById<RowFlexLayout>(R.id.buttonsContainer)
                    .realChildren.first().performClick()
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
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        var catchedActionButton: InAppMessagePayloadButton? = null
        var catchedClosedByUser: Boolean? = null
        var catchedError: String? = null
        var catchedCancelButton: InAppMessagePayloadButton? = null
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(bitmapCache, fontCache)
        val view = presenter.getView(
            activity.get(),
            inAppMessageType,
            payload,
            payload?.let { uiPayloadBuilder.build(it) },
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
                val realView = view as InAppMessageRichstyleDialog
                // find second button - is cancel
                realView.findViewById<RowFlexLayout>(R.id.buttonsContainer).realChildren[1].performClick()
            }
            ALERT -> {
                val realView = view as InAppMessageAlert
                realView.dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
            }
            SLIDE_IN -> {
                val realView = view as InAppMessageRichstyleSlideIn
                // find second button - is cancel
                realView.contentView.findViewById<RowFlexLayout>(R.id.buttonsContainer).realChildren[1].performClick()
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
            FREEFORM -> {
                assertNotNull(catchedCancelButton)
                assertEquals(
                    payloadHtml?.actions?.find { it.actionType == HtmlActionType.CLOSE }?.buttonText,
                    catchedCancelButton!!.text
                )
            }
            ALERT, SLIDE_IN, MODAL, FULLSCREEN -> {
                assertNotNull(catchedCancelButton)
                assertEquals(
                    payload?.buttons?.first { it.buttonType == InAppMessageButtonType.CANCEL }?.text,
                    catchedCancelButton!!.text
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
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        var catchedActionButton: InAppMessagePayloadButton? = null
        var catchedClosedByUser: Boolean? = null
        var catchedError: String? = null
        var catchedCancelButton: InAppMessagePayloadButton? = null
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(bitmapCache, fontCache)
        val view = presenter.getView(
            activity.get(),
            inAppMessageType,
            payload,
            payload?.let { uiPayloadBuilder.build(it) },
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
        val realView = view as InAppMessageRichstyleSlideIn
        val containerView = realView.contentView.findViewById<CardView>(R.id.inAppMessageSlideInContainer)
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
        val presenter = InAppMessagePresenter(
            activity.get(),
            bitmapCache
        )
        var catchedActionButton: InAppMessagePayloadButton? = null
        var catchedClosedByUser: Boolean? = null
        var catchedError: String? = null
        var catchedCancelButton: InAppMessagePayloadButton? = null
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(bitmapCache, fontCache)
        val view = presenter.getView(
            activity.get(),
            inAppMessageType,
            payload,
            payload?.let { uiPayloadBuilder.build(it) },
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
