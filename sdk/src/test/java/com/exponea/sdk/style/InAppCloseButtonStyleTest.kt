package com.exponea.sdk.style

import android.graphics.Color
import android.util.TypedValue
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.testutil.FakeDrawableCache
import com.exponea.sdk.testutil.FakeFontCache
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppCloseButtonStyleTest {

    @Test
    fun `should parse complete style`() {
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(
            drawableCache = FakeDrawableCache(),
            fontCache = FakeFontCache()
        )
        val uiPayload = uiPayloadBuilder.build(InAppMessageTest.buildInAppMessageWithRichstyle().payload!!)
        val parsed = uiPayload!!.closeButton.style
        assertEquals(50, parsed.margin.top.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.top.unit)
        assertEquals(10, parsed.margin.right.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.right.unit)
        assertEquals(50, parsed.margin.bottom.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.bottom.unit)
        assertEquals(10, parsed.margin.left.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.left.unit)
        assertEquals(InAppRichstylePayloadBuilder.DEFAULT_CLOSE_BUTTON_PADDING, parsed.padding)
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.padding.top.unit)
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.padding.left.unit)
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.padding.right.unit)
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.padding.bottom.unit)
        assertEquals(InAppRichstylePayloadBuilder.DEFAULT_CLOSE_BUTTON_SIZE, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.size.unit)
        assertEquals(Color.YELLOW, parsed.backgroundColor)
        assertEquals(Color.WHITE, parsed.iconColor)
        assertTrue(parsed.enabled)
    }
}
