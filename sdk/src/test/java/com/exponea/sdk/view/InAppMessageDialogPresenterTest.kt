package com.exponea.sdk.view

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.models.InAppMessageType
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.Robolectric.buildActivity

@RunWith(ParameterizedRobolectricTestRunner::class)
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

    private class MockActivity : Activity()
    private val payload = InAppMessageTest.getInAppMessage().payload

    @Test
    fun `should not show dialog without activity`() {
        val presenter = InAppMessageDialogPresenter(ApplicationProvider.getApplicationContext())
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }

    @Test
    fun `should show dialog with activity`() {
        val presenter = InAppMessageDialogPresenter(ApplicationProvider.getApplicationContext())
        buildActivity(MockActivity::class.java).setup().resume()
        assertNotNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }

    @Test
    fun `should not show dialog after activity is paused`() {
        val presenter = InAppMessageDialogPresenter(ApplicationProvider.getApplicationContext())
        buildActivity(MockActivity::class.java).setup().resume()
        val presented = presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {})
        assertNotNull(presented)
        presented.dismiss()
        buildActivity(MockActivity::class.java).setup().resume().pause()
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }

    @Test
    fun `should only show one dialog at a time`() {
        val presenter = InAppMessageDialogPresenter(ApplicationProvider.getApplicationContext())
        buildActivity(MockActivity::class.java).setup().resume()
        val presented = presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {})
        assertNotNull(presented)
        assertNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
        presented.dismiss()
        Robolectric.flushForegroundThreadScheduler() // skip animations
        assertNotNull(presenter.show(inAppMessageType, payload, BitmapFactory.decodeFile("mock-file"), {}, {}))
    }
}
