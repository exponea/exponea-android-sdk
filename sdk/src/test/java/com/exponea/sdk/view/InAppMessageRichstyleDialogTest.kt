package com.exponea.sdk.view

import android.graphics.Typeface
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.R
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.style.ImageSizing
import com.exponea.sdk.style.InAppRichstylePayloadBuilder
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.view.layout.RowFlexLayout
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppMessageRichstyleDialogTest {

    @Before
    fun setUp() {
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
    }

    private fun showDialogForPayload(
        payload: InAppMessagePayload,
        fullscreen: Boolean = true
    ): InAppMessageRichstyleDialog {
        val bitmapCache = DrawableCacheImpl(ApplicationProvider.getApplicationContext())
        val fontCache = FontCacheImpl(ApplicationProvider.getApplicationContext())
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(
            drawableCache = bitmapCache,
            fontCache = fontCache
        )
        val dialog = InAppMessageRichstyleDialog(
            ApplicationProvider.getApplicationContext(),
            fullscreen,
            uiPayloadBuilder.build(payload)!!,
            {},
            { _, _ -> },
            {}
        )
        dialog.show()
        // positioning of buttons are done while measuring
        val buttonContainer = dialog.findViewById<RowFlexLayout>(R.id.buttonsContainer)
        buttonContainer.measure(makeMeasureSpec(1000, AT_MOST), makeMeasureSpec(1000, AT_MOST))
        return dialog
    }

    private fun validateVisibility(root: InAppMessageRichstyleDialog, viewIds: List<Int>) {
        viewIds.forEach {
            val view = root.findViewById<View>(it)
                ?: fail("View with id $it not found")
            if (view.visibility != View.VISIBLE) {
                fail("View with id $it should be visible")
            }
        }
    }

    private fun validateNonVisibility(root: InAppMessageRichstyleDialog, viewIds: List<Int>) {
        viewIds.forEach {
            val view = root.findViewById<View>(it)
                ?: fail("View with id $it not found")
            if (view.visibility == View.VISIBLE) {
                fail("View with id $it should not be visible")
            }
        }
    }

    @Test
    fun `should setup dialog with default payload`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!
        val dialog = showDialogForPayload(payload)
        assertEquals(payload.title, dialog.findViewById<TextView>(R.id.textViewTitle).text)
        assertEquals(payload.bodyText, dialog.findViewById<TextView>(R.id.textViewBody).text)
        validateVisibility(dialog, listOf(R.id.inAppMessageDialogTopImage))
        validateNonVisibility(dialog, listOf(
            R.id.inAppMessageDialogBottomImage,
            R.id.inAppMessageDialogBackgroundImage
        ))
        val buttonContainer = dialog.findViewById<RowFlexLayout>(R.id.buttonsContainer)
        val button = (buttonContainer.children.first() as LinearLayout).children.first() as Button
        assertEquals(payload.buttons!![0].text, button.text)
    }

    @Test
    fun `should setup image overlay by sizing`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            imageSizing = ImageSizing.FULLSCREEN.value
        ).payload!!
        val dialog = showDialogForPayload(payload)
        validateVisibility(dialog, listOf(
            R.id.inAppMessageDialogBackgroundImage
        ))
        validateNonVisibility(dialog, listOf(
            R.id.inAppMessageDialogBottomImage,
            R.id.inAppMessageDialogTopImage
        ))
    }

    @Test
    fun `should setup image overlay by text_over_image`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isTextOverImage = true
        ).payload!!
        val dialog = showDialogForPayload(payload)
        validateVisibility(dialog, listOf(
            R.id.inAppMessageDialogBackgroundImage
        ))
        validateNonVisibility(dialog, listOf(
            R.id.inAppMessageDialogBottomImage,
            R.id.inAppMessageDialogTopImage
        ))
    }

    @Test
    fun `should setup image overlay primary by text_over_image`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isTextOverImage = true,
            textPosition = "bottom"
        ).payload!!
        val dialog = showDialogForPayload(payload)
        validateVisibility(dialog, listOf(
            R.id.inAppMessageDialogBackgroundImage
        ))
        validateNonVisibility(dialog, listOf(
            R.id.inAppMessageDialogBottomImage,
            R.id.inAppMessageDialogTopImage
        ))
    }

    @Test
    fun `should setup image into top position`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            textPosition = "bottom"
        ).payload!!
        val dialog = showDialogForPayload(payload)
        validateVisibility(dialog, listOf(
            R.id.inAppMessageDialogTopImage
        ))
        validateNonVisibility(dialog, listOf(
            R.id.inAppMessageDialogBottomImage,
            R.id.inAppMessageDialogBackgroundImage
        ))
    }

    @Test
    fun `should setup image into bottom position`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            textPosition = "top"
        ).payload!!
        val dialog = showDialogForPayload(payload)
        validateVisibility(dialog, listOf(
            R.id.inAppMessageDialogBottomImage
        ))
        validateNonVisibility(dialog, listOf(
            R.id.inAppMessageDialogTopImage,
            R.id.inAppMessageDialogBackgroundImage
        ))
    }

    @Test
    fun `should hide image if not enabled`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isImageEnabled = false
        ).payload!!
        val dialog = showDialogForPayload(payload)
        validateNonVisibility(dialog, listOf(
            R.id.inAppMessageDialogBottomImage,
            R.id.inAppMessageDialogTopImage,
            R.id.inAppMessageDialogBackgroundImage
        ))
    }

    @Test
    fun `should hide title if not enabled`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isTitleEnabled = false
        ).payload!!
        val dialog = showDialogForPayload(payload)
        validateNonVisibility(dialog, listOf(
            R.id.textViewTitle
        ))
    }

    @Test
    fun `should hide body text if not enabled`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isBodyEnabled = false
        ).payload!!
        val dialog = showDialogForPayload(payload)
        validateNonVisibility(dialog, listOf(
            R.id.textViewBody
        ))
    }

    @Test
    fun `should setup fullscreen dialog to match parent`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!
        val dialog = showDialogForPayload(payload, fullscreen = true)
        assertEquals(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dialog.findViewById<View>(R.id.inAppMessageDialogContainer).layoutParams.height
        )
        assertEquals(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dialog.findViewById<View>(R.id.inAppMessageDialogBody).layoutParams.height
        )
    }

    @Test
    fun `should setup modal dialog to wrap content`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!
        val dialog = showDialogForPayload(payload, fullscreen = false)
        assertEquals(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dialog.findViewById<View>(R.id.inAppMessageDialogContainer).layoutParams.height
        )
        assertEquals(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dialog.findViewById<View>(R.id.inAppMessageDialogBody).layoutParams.height
        )
    }
}
