package com.exponea.sdk.style

import android.content.Context
import android.content.pm.ProviderInfo
import android.graphics.Color
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.repository.InAppMessageBitmapCacheImpl
import com.exponea.sdk.services.ExponeaContextProvider
import io.mockk.every
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppButtonStyleTest {

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
        mockkConstructorFix(FontCacheImpl::class) {
            every { anyConstructed<FontCacheImpl>().has(any()) }
        }
        every { anyConstructed<FontCacheImpl>().getFile(any()) } returns File(
            this.javaClass.classLoader!!.getResource("style/xtrusion.ttf")!!.file
        )
        every { anyConstructed<FontCacheImpl>().has(any()) } returns true
        uiPayloadBuilder = InAppRichstylePayloadBuilder(
            drawableCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext()),
            fontCache = FontCacheImpl(ApplicationProvider.getApplicationContext())
        )
    }

    @Test
    fun `should parse complete style`() {
        val uiPayload = uiPayloadBuilder.build(InAppMessageTest.buildInAppMessageWithRichstyle().payload!!)
        val parsed = uiPayload!!.buttons[0].style
        assertNotNull(parsed.customTypeface)
        assertEquals(ButtonSizing.HUG_TEXT, parsed.sizing)
        assertEquals(Color.BLUE, parsed.backgroundColor)
        assertEquals(12, parsed.cornerRadius.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.cornerRadius.unit)
        assertEquals(20, parsed.margin.top.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.top.unit)
        assertEquals(10, parsed.margin.right.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.right.unit)
        assertEquals(15, parsed.margin.bottom.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.bottom.unit)
        assertEquals(10, parsed.margin.left.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.left.unit)
        assertEquals(24, parsed.textSize.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.textSize.unit)
        assertEquals(32, parsed.lineHeight.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.lineHeight.unit)
        assertEquals(20, parsed.padding.top.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.padding.top.unit)
        assertEquals(10, parsed.padding.right.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.padding.right.unit)
        assertEquals(15, parsed.padding.bottom.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.padding.bottom.unit)
        assertEquals(10, parsed.padding.left.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.padding.left.unit)
        assertEquals(Color.WHITE, parsed.textColor)
        assertEquals(listOf(TextStyle.BOLD), parsed.textStyle)
    }
}
