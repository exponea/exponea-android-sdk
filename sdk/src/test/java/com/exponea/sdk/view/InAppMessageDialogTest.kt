package com.exponea.sdk.view

import android.content.Context
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.R
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.testutil.MockFile
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageDialogTest {
    @Test
    fun `should setup dialog`() {
        val payload = InAppMessageTest.buildInAppMessage().payload
        val imageCache = mockk<DrawableCache>()
        every { imageCache.showImage(any(), any(), any()) } just Runs
        every { imageCache.has(any()) } returns true
        every { imageCache.getFile(any()) } returns MockFile()
        val dialog = InAppMessageDialog(
            ApplicationProvider.getApplicationContext<Context>(),
            true,
            payload!!,
            imageCache,
            {},
            { _, _ -> },
            {}
        )
        dialog.show()
        assertEquals(payload.title, dialog.findViewById<TextView>(R.id.textViewTitle).text)
        assertEquals(payload.bodyText, dialog.findViewById<TextView>(R.id.textViewBody).text)
        assertEquals(payload.buttons!![0].buttonText, dialog.findViewById<Button>(R.id.buttonAction1).text)
    }
}
