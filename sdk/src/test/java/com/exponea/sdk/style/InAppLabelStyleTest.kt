package com.exponea.sdk.style

import android.content.Context
import android.content.pm.ProviderInfo
import android.graphics.Color
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.InAppMessagePayload
import com.exponea.sdk.services.ExponeaContextProvider
import com.exponea.sdk.testutil.FakeDrawableCache
import com.exponea.sdk.testutil.FakeFontCache
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppLabelStyleTest {

    private companion object {
        const val TITLE_FONT_URL = "https://cdn.example.test/fonts/title.ttf"
    }

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
        val fontCache = FakeFontCache()
        val parsedTitleStyle = InAppRichstylePayloadBuilder(
            drawableCache = FakeDrawableCache(),
            fontCache = fontCache
        ).build(
            InAppMessagePayload(
                titleFontUrl = TITLE_FONT_URL,
                titleTextSize = "24px",
                titleTextAlignment = "center",
                titleTextStyle = listOf("bold"),
                titleTextColor = "red",
                titleLineHeight = "32px",
                titlePadding = "20px 10px 15px 10px"
            )
        )!!.title.style

        assertNotNull(parsedTitleStyle.customTypeface)
        assertThat(fontCache.requestedUrls, hasItem(TITLE_FONT_URL))
        assertEquals(24, parsedTitleStyle.textSize.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedTitleStyle.textSize.unit)
        assertEquals(TextAlignment.CENTER, parsedTitleStyle.textAlignment)
        assertEquals(listOf(TextStyle.BOLD), parsedTitleStyle.textStyle)
        assertEquals(Color.RED, parsedTitleStyle.textColor)
        assertEquals(32, parsedTitleStyle.lineHeight.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedTitleStyle.lineHeight.unit)
        assertEquals(20, parsedTitleStyle.padding.top.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedTitleStyle.padding.top.unit)
        assertEquals(10, parsedTitleStyle.padding.right.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedTitleStyle.padding.right.unit)
        assertEquals(15, parsedTitleStyle.padding.bottom.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedTitleStyle.padding.bottom.unit)
        assertEquals(10, parsedTitleStyle.padding.left.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsedTitleStyle.padding.left.unit)
    }
}
