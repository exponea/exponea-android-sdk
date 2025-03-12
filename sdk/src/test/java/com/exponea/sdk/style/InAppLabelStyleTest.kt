package com.exponea.sdk.style

import android.content.Context
import android.content.pm.ProviderInfo
import android.graphics.Color
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.mockkConstructorFix
import com.exponea.sdk.models.InAppMessagePayload
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
internal class InAppLabelStyleTest {

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
    }

    @Test
    fun `should parse complete style`() {
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(
            drawableCache = InAppMessageBitmapCacheImpl(ApplicationProvider.getApplicationContext()),
            fontCache = FontCacheImpl(ApplicationProvider.getApplicationContext())
        )
        val uiPayload = uiPayloadBuilder.build(InAppMessagePayload(
            titleFontUrl = "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
            titleTextSize = "24px",
            titleTextAlignment = "center",
            titleTextStyle = listOf("bold"),
            titleTextColor = "red",
            titleLineHeight = "32px",
            titlePadding = "20px 10px 15px 10px"
        ))
        val parsed = uiPayload!!.title.style
        assertNotNull(parsed.customTypeface)
        assertEquals(24, parsed.textSize.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.textSize.unit)
        assertEquals(TextAlignment.CENTER, parsed.textAlignment)
        assertEquals(listOf(TextStyle.BOLD), parsed.textStyle)
        assertEquals(Color.RED, parsed.textColor)
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
    }
}
