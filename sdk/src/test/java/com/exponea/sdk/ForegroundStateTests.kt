package com.exponea.sdk

import android.content.Context
import android.content.pm.ProviderInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.testutil.mocks.MockApplication
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MockApplication::class)
internal class ForegroundStateTests {

    @Before
    fun init() {
        ExponeaContextProvider.applicationIsForeground = false
        Robolectric
            .buildContentProvider(ExponeaContextProvider::class.java)
            .create(ProviderInfo().apply {
                authority = "${ApplicationProvider.getApplicationContext<Context>().packageName}.sdk.contextprovider"
                grantUriPermissions = true
            }).get()
    }

    @Test
    fun `should detect foreground state for started activity until stopped`() {
        val controller = buildActivity(AppCompatActivity::class.java)
        assertFalse(ExponeaContextProvider.applicationIsForeground)
        controller.create()
        assertFalse(ExponeaContextProvider.applicationIsForeground)
        controller.start()
        assertTrue(ExponeaContextProvider.applicationIsForeground)
        controller.postCreate(null)
        assertTrue(ExponeaContextProvider.applicationIsForeground)
        controller.resume()
        assertTrue(ExponeaContextProvider.applicationIsForeground)
        controller.postResume()
        assertTrue(ExponeaContextProvider.applicationIsForeground)
        controller.visible()
        assertTrue(ExponeaContextProvider.applicationIsForeground)
        controller.topActivityResumed(true)
        assertTrue(ExponeaContextProvider.applicationIsForeground)
        controller.pause()
        assertTrue(ExponeaContextProvider.applicationIsForeground)
        controller.stop()
        assertFalse(ExponeaContextProvider.applicationIsForeground)
        controller.destroy()
        assertFalse(ExponeaContextProvider.applicationIsForeground)
    }
}
