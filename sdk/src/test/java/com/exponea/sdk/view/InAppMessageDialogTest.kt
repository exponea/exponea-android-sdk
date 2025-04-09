package com.exponea.sdk.view

import android.content.Context
import android.content.pm.ProviderInfo
import android.graphics.Typeface
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.R
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.repository.DrawableCache
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.style.InAppRichstylePayloadBuilder
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.view.layout.RowFlexLayout
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.File
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageDialogTest {

    lateinit var uiPayloadBuilder: InAppRichstylePayloadBuilder

    @Before
    fun before() {
        ExponeaContextProvider.applicationIsForeground = false
        Robolectric
            .buildContentProvider(ExponeaContextProvider::class.java)
            .create(ProviderInfo().apply {
                authority = "${ApplicationProvider.getApplicationContext<Context>().packageName}.sdk.contextprovider"
                grantUriPermissions = true
            }).get()
        mockkConstructorFix(DrawableCacheImpl::class) {
            every { anyConstructed<DrawableCacheImpl>().has(any()) }
        }
        every { anyConstructed<DrawableCacheImpl>().showImage(any(), any(), any()) } just Runs
        every { anyConstructed<DrawableCacheImpl>().has(any()) } returns true
        every { anyConstructed<DrawableCacheImpl>().getFile(any()) } returns MockFile()
        every {
            anyConstructed<DrawableCacheImpl>().getDrawable(any<String>())
        } returns AppCompatDrawableManager.get().getDrawable(
            ApplicationProvider.getApplicationContext(),
            R.drawable.in_app_message_close_button
        )
        every {
            anyConstructed<DrawableCacheImpl>().preload(any<List<String>>(), any())
        } answers {
            secondArg<((Boolean) -> Unit)?>()?.invoke(true)
        }
        mockkConstructorFix(FontCacheImpl::class) {
            every { anyConstructed<FontCacheImpl>().has(any()) }
        }
        every { anyConstructed<FontCacheImpl>().getFontFile(any()) } returns File(
            this.javaClass.classLoader!!.getResource("xtrusion.ttf")!!.file
        )
        every { anyConstructed<FontCacheImpl>().getTypeface(any()) } returns Typeface.createFromFile(
            this.javaClass.classLoader!!.getResource("xtrusion.ttf")!!.file
        )
        every { anyConstructed<FontCacheImpl>().has(any()) } returns true
        every {
            anyConstructed<FontCacheImpl>().preload(any<List<String>>(), any())
        } answers {
            secondArg<((Boolean) -> Unit)?>()?.invoke(true)
        }
        uiPayloadBuilder = InAppRichstylePayloadBuilder(
            drawableCache = DrawableCacheImpl(ApplicationProvider.getApplicationContext()),
            fontCache = FontCacheImpl(ApplicationProvider.getApplicationContext())
        )
    }

    @Test
    fun `should setup non-richstyle dialog`() {
        val payload = InAppMessageTest.buildInAppMessageWithoutRichstyle().payload
        val imageCache = mockk<DrawableCache>()
        every { imageCache.showImage(any(), any(), any()) } just Runs
        every { imageCache.has(any()) } returns true
        every { imageCache.getFile(any()) } returns MockFile()
        val dialog = InAppMessageDialog(
            buildActivity(InAppMessageActivity::class.java).get(),
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
        assertEquals(payload.buttons!![0].text, dialog.findViewById<Button>(R.id.buttonAction1).text)
    }

    @Test
    fun `should setup richstyle dialog`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!
        val uiPayload = uiPayloadBuilder.build(payload)
        val dialog = InAppMessageRichstyleDialog(
            buildActivity(InAppMessageActivity::class.java).get(),
            true,
            uiPayload!!,
            {},
            { _, _ -> },
            {}
        )
        dialog.show()
        // positioning of buttons are done while measuring
        val buttonContainer = dialog.findViewById<RowFlexLayout>(R.id.buttonsContainer)
        buttonContainer.measure(makeMeasureSpec(1000, AT_MOST), makeMeasureSpec(1000, AT_MOST))
        assertEquals(payload.title, dialog.findViewById<TextView>(R.id.textViewTitle).text)
        assertEquals(payload.bodyText, dialog.findViewById<TextView>(R.id.textViewBody).text)
        val button = (buttonContainer.children.first() as LinearLayout).children.first() as Button
        assertEquals(payload.buttons!![0].text, button.text)
    }
}
