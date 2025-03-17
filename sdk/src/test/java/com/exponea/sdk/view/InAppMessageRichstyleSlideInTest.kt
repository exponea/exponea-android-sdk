package com.exponea.sdk.view

import android.graphics.Typeface
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.makeMeasureSpec
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.core.view.children
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.R
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.repository.SimpleFileCache
import com.exponea.sdk.style.ImageSizing
import com.exponea.sdk.style.InAppRichstylePayloadBuilder
import com.exponea.sdk.testutil.MockFile
import com.exponea.sdk.testutil.mocks.MockApplication
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = MockApplication::class)
internal class InAppMessageRichstyleSlideInTest {

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
        mockkConstructorFix(SimpleFileCache::class) {
            every { anyConstructed<SimpleFileCache>().has(any()) }
        }
        every { anyConstructed<SimpleFileCache>().getFile(any()) } returns File(
            this.javaClass.classLoader!!.getResource("xtrusion.ttf")!!.file
        )
        mockkConstructorFix(FontCacheImpl::class) {
            every { anyConstructed<FontCacheImpl>().has(any()) }
        }
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

    private fun showSlideInForPayload(payload: InAppMessagePayload): InAppMessageRichstyleSlideIn {
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().resume()
        val bitmapCache = DrawableCacheImpl(ApplicationProvider.getApplicationContext())
        val fontCache = FontCacheImpl(ApplicationProvider.getApplicationContext())
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(
            drawableCache = bitmapCache,
            fontCache = fontCache
        )
        val slideIn = InAppMessageRichstyleSlideIn(
            activity.get(),
            uiPayloadBuilder.build(payload)!!,
            {},
            { _, _ -> },
            {}
        )
        slideIn.show()
        // positioning of buttons are done while measuring
        val buttonContainer = slideIn.contentView.findViewById<RowFlexLayout>(R.id.buttonsContainer)
        buttonContainer.measure(makeMeasureSpec(1000, AT_MOST), makeMeasureSpec(1000, AT_MOST))
        return slideIn
    }

    private fun validateVisibility(root: InAppMessageRichstyleSlideIn, viewIds: List<Int>) {
        viewIds.forEach {
            val view = root.contentView.findViewById<View>(it)
                ?: fail("View with id $it not found")
            if (view.visibility != View.VISIBLE) {
                fail("View with id $it should be visible")
            }
        }
    }

    private fun validateNonVisibility(root: InAppMessageRichstyleSlideIn, viewIds: List<Int>) {
        viewIds.forEach {
            val view = root.contentView.findViewById<View>(it)
                ?: fail("View with id $it not found")
            if (view.visibility == View.VISIBLE) {
                fail("View with id $it should not be visible")
            }
        }
    }

    @Test
    fun `should setup dialog with default payload`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle().payload!!
        val slideIn = showSlideInForPayload(payload)
        assertEquals(payload.title, slideIn.contentView.findViewById<TextView>(R.id.textViewTitle).text)
        assertEquals(payload.bodyText, slideIn.contentView.findViewById<TextView>(R.id.textViewBody).text)
        validateVisibility(slideIn, listOf(R.id.inAppMessageSlideInLeftImage))
        validateNonVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInRightImage,
            R.id.inAppMessageSlideInBackgroundImage
        ))
        val buttonContainer = slideIn.contentView.findViewById<RowFlexLayout>(R.id.buttonsContainer)
        val button = (buttonContainer.children.first() as LinearLayout).children.first() as Button
        assertEquals(payload.buttons!![0].text, button.text)
    }

    @Test
    fun `should setup image overlay by sizing`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            imageSizing = ImageSizing.FULLSCREEN.value
        ).payload!!
        val slideIn = showSlideInForPayload(payload)
        validateVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInBackgroundImage
        ))
        validateNonVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInRightImage,
            R.id.inAppMessageSlideInLeftImage
        ))
    }

    @Test
    fun `should setup image overlay by text_over_image`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isTextOverImage = true
        ).payload!!
        val slideIn = showSlideInForPayload(payload)
        validateVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInBackgroundImage
        ))
        validateNonVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInRightImage,
            R.id.inAppMessageSlideInLeftImage
        ))
    }

    @Test
    fun `should setup image overlay primary by text_over_image`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isTextOverImage = true,
            textPosition = "right"
        ).payload!!
        val slideIn = showSlideInForPayload(payload)
        validateVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInBackgroundImage
        ))
        validateNonVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInRightImage,
            R.id.inAppMessageSlideInLeftImage
        ))
    }

    @Test
    fun `should setup image into top position`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            textPosition = "right"
        ).payload!!
        val slideIn = showSlideInForPayload(payload)
        validateVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInLeftImage
        ))
        validateNonVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInRightImage,
            R.id.inAppMessageSlideInBackgroundImage
        ))
    }

    @Test
    fun `should setup image into bottom position`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            textPosition = "left"
        ).payload!!
        val slideIn = showSlideInForPayload(payload)
        validateVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInRightImage
        ))
        validateNonVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInLeftImage,
            R.id.inAppMessageSlideInBackgroundImage
        ))
    }

    @Test
    fun `should hide image if not enabled`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isImageEnabled = false
        ).payload!!
        val slideIn = showSlideInForPayload(payload)
        validateNonVisibility(slideIn, listOf(
            R.id.inAppMessageSlideInRightImage,
            R.id.inAppMessageSlideInLeftImage,
            R.id.inAppMessageSlideInBackgroundImage
        ))
    }

    @Test
    fun `should hide title if not enabled`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isTitleEnabled = false
        ).payload!!
        val slideIn = showSlideInForPayload(payload)
        validateNonVisibility(slideIn, listOf(
            R.id.textViewTitle
        ))
    }

    @Test
    fun `should hide body text if not enabled`() {
        val payload = InAppMessageTest.buildInAppMessageWithRichstyle(
            isBodyEnabled = false
        ).payload!!
        val slideIn = showSlideInForPayload(payload)
        validateNonVisibility(slideIn, listOf(
            R.id.textViewBody
        ))
    }
}
