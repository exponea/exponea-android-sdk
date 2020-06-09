package com.exponea.sdk.view

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.InAppMessageType
import com.exponea.sdk.testutil.mocks.MockApplication
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Robolectric.buildActivity
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
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }

    @Test
    fun `should show dialog once and activity is resumed`() {
        val presenter = InAppMessagePresenter(ApplicationProvider.getApplicationContext())
        buildActivity(AppCompatActivity::class.java).setup().resume()
        assertNotNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }

    @Test
    fun `should show dialog when initialized with resumed activity`() {
        val activity = buildActivity(AppCompatActivity::class.java).setup().resume()
        val presenter = InAppMessagePresenter(activity.get())
        assertNotNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }

    @Test
    fun `should not show dialog after activity is paused`() {
        val presenter = InAppMessagePresenter(ApplicationProvider.getApplicationContext())
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented = presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {})
        assertNotNull(presented)
        presented.dismissedCallback()
        buildActivity(AppCompatActivity::class.java).setup().resume().pause()
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }

    @Test
    fun `should only show one dialog at a time`() {
        val presenter = InAppMessagePresenter(ApplicationProvider.getApplicationContext())
        buildActivity(AppCompatActivity::class.java).setup().resume()
        val presented = presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {})
        assertNotNull(presented)
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
        presented.dismissedCallback()
        Robolectric.flushForegroundThreadScheduler() // skip animations
        assertNotNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }
}
