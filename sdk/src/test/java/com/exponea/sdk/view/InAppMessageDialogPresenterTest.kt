package com.exponea.sdk.view

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.Exponea
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.testutil.mocks.MockApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Robolectric.buildActivity
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = MockApplication::class)
internal class InAppMessageDialogPresenterTest(
    val inAppMessageType: InAppMessageType
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun data(): List<Array<out Any?>> {
            return InAppMessageType.values().map { arrayOf(it) }
        }
    }

    private val payload = InAppMessageTest.getInAppMessage().payload

    @Test
    fun `should not show dialog without resumed activity`() {
        buildActivity(AppCompatActivity::class.java).setup()
        val presenter = InAppMessagePresenter(ApplicationProvider.getApplicationContext())
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), null, mockk(), {}))
    }

    @Test
    fun `should show dialog once and activity is resumed`() {
        val presenter = InAppMessagePresenter(ApplicationProvider.getApplicationContext())
        buildActivity(AppCompatActivity::class.java).setup().resume()
        assertNotNull(
            presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), null, mockk(), {})
        )
    }

    @Test
    fun `should show dialog when initialized with resumed activity`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val presenter = InAppMessagePresenter(activity.get())
        assertNotNull(
            presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), null, mockk(), {})
        )
    }

    @Test
    fun `should not show dialog after activity is paused`() {
        val presenter = InAppMessagePresenter(ApplicationProvider.getApplicationContext())
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented =
            presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), null, mockk(), {})
        assertNotNull(presented)
        presented.dismissedCallback()
        buildActivity(AppCompatActivity::class.java).setup().resume().pause()
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), null, mockk(), {}))
    }

    @Test
    fun `should only show one dialog at a time`() {
        val presenter = InAppMessagePresenter(ApplicationProvider.getApplicationContext())
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented =
            presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), null, mockk(), {})
        assertNotNull(presented)
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), null, mockk(), {}))
        presented.dismissedCallback()
        Robolectric.flushForegroundThreadScheduler() // skip animations
        assertNotNull(
            presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), null, mockk(), {})
        )
    }

    @Test
    fun `should dismiss the dialog after timeout has passed`() {
        if (inAppMessageType == InAppMessageType.SLIDE_IN) {
            // we'll skip this for slide-in that requires rendering of the dialog itself
            return
        }
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val presenter = InAppMessagePresenter(activity.get())
        val dismiss = spyk<() -> Unit>()
        val presented =
            presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), 1234, mockk(), dismiss)

        mockkObject(Exponea)
        every { Exponea.presentedInAppMessage } returns presented
        val intent = shadowOf(activity.get()).peekNextStartedActivity()
        buildActivity(InAppMessageActivity::class.java, intent).create().resume()

        Robolectric.getForegroundThreadScheduler().advanceBy(1233, TimeUnit.MILLISECONDS)
        verify(exactly = 0) { dismiss() }
        Robolectric.getForegroundThreadScheduler().advanceBy(1, TimeUnit.MILLISECONDS)
        verify(exactly = 1) { dismiss() }
    }
}
