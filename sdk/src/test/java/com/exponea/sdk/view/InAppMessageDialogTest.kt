package com.exponea.sdk.view

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.R
import com.exponea.sdk.models.InAppMessageTest
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageDialogTest {
    @Test
    fun `should setup dialog`() {
        val payload = InAppMessageTest.getInAppMessage().payload
        val dialog = InAppMessageDialog(
            ApplicationProvider.getApplicationContext<Context>(),
            true,
            payload,
            BitmapFactory.decodeFile("mock-file"),
            {},
            {}
        )
        dialog.show()
        assertEquals(payload.title, dialog.findViewById<TextView>(R.id.textViewTitle).text)
        assertEquals(payload.bodyText, dialog.findViewById<TextView>(R.id.textViewBody).text)
        assertEquals(payload.buttonText, dialog.findViewById<Button>(R.id.buttonAction).text)
    }
}
