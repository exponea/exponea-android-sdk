package com.exponea.sdk.view

import android.app.Activity
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.InAppMessageTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageDialogPresenterTest {
    private class MockActivity : Activity()
    private val payload = InAppMessageTest.getInAppMessage().payload

    @Test
    fun `should not show dialog without activity`() {
        val presenter = InAppMessageDialogPresenter(ApplicationProvider.getApplicationContext())
        assertFalse(presenter.show(payload, BitmapFactory.decodeFile("mock-file")))
    }

    @Test
    fun `should show dialog with activity`() {
        val presenter = InAppMessageDialogPresenter(ApplicationProvider.getApplicationContext())
        buildActivity(MockActivity::class.java).setup().resume()
        assertTrue(presenter.show(payload, BitmapFactory.decodeFile("mock-file")))
    }
    @Test
    fun `should not show dialog after activity is paused`() {
        val presenter = InAppMessageDialogPresenter(ApplicationProvider.getApplicationContext())
        buildActivity(MockActivity::class.java).setup().resume()
        assertTrue(presenter.show(payload, BitmapFactory.decodeFile("mock-file")))
        buildActivity(MockActivity::class.java).setup().resume().pause()
        assertFalse(presenter.show(payload, BitmapFactory.decodeFile("mock-file")))
    }
}
