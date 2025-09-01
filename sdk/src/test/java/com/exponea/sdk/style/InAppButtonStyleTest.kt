package com.exponea.sdk.style

import android.content.Context
import android.content.pm.ProviderInfo
import android.graphics.Color
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.repository.DrawableCacheImpl
import com.exponea.sdk.repository.FontCacheImpl
import com.exponea.sdk.services.ExponeaContextProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppButtonStyleTest {

    @Before
    fun before() {
        ExponeaContextProvider.applicationIsForeground = false
        Robolectric
            .buildContentProvider(ExponeaContextProvider::class.java)
            .create(ProviderInfo().apply {
                authority = "${ApplicationProvider.getApplicationContext<Context>().packageName}.sdk.contextprovider"
                grantUriPermissions = true
            }).get()
    }

    @Test
    fun `should parse complete style`() {
        val parsedButtonStyle = InAppRichstylePayloadBuilder(
            drawableCache = DrawableCacheImpl(ApplicationProvider.getApplicationContext()),
            fontCache = FontCacheImpl(ApplicationProvider.getApplicationContext())
        ).build(InAppMessageTest.buildInAppMessageWithRichstyle().payload!!)!!.buttons[0].style

        assertNotNull(parsedButtonStyle.customTypeface)
        assertEquals(ButtonSizing.HUG_TEXT, parsedButtonStyle.sizing)
        assertEquals(Color.BLUE, parsedButtonStyle.backgroundColor)
        assertEquals(12, parsedButtonStyle.cornerRadius.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.cornerRadius.unit)
        assertEquals(20, parsedButtonStyle.margin.top.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.margin.top.unit)
        assertEquals(10, parsedButtonStyle.margin.right.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.margin.right.unit)
        assertEquals(15, parsedButtonStyle.margin.bottom.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.margin.bottom.unit)
        assertEquals(10, parsedButtonStyle.margin.left.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.margin.left.unit)
        assertEquals(24, parsedButtonStyle.textSize.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.textSize.unit)
        assertEquals(32, parsedButtonStyle.lineHeight.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.lineHeight.unit)
        assertEquals(20, parsedButtonStyle.padding.top.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.padding.top.unit)
        assertEquals(10, parsedButtonStyle.padding.right.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.padding.right.unit)
        assertEquals(15, parsedButtonStyle.padding.bottom.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.padding.bottom.unit)
        assertEquals(10, parsedButtonStyle.padding.left.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedButtonStyle.padding.left.unit)
        assertEquals(Color.WHITE, parsedButtonStyle.textColor)
        assertEquals(listOf(TextStyle.BOLD), parsedButtonStyle.textStyle)
    }
}
