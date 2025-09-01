package com.exponea.sdk.style

import android.content.Context
import android.content.pm.ProviderInfo
import android.graphics.Color
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import com.exponea.sdk.models.InAppMessagePayload
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
    }

    @Test
    fun `should parse complete style`() {
        val appContext: Context = ApplicationProvider.getApplicationContext()

        val parsedTitleStyle = InAppRichstylePayloadBuilder(
            drawableCache = DrawableCacheImpl(appContext),
            fontCache = FontCacheImpl(appContext)
        ).build(
            InAppMessagePayload(
                titleFontUrl = "https://webpagepublicity.com/free-fonts/x/Xtrusion%20(BRK).ttf",
                titleTextSize = "24px",
                titleTextAlignment = "center",
                titleTextStyle = listOf("bold"),
                titleTextColor = "red",
                titleLineHeight = "32px",
                titlePadding = "20px 10px 15px 10px"
            )
        )!!.title.style

        assertNotNull(parsedTitleStyle.customTypeface)
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
