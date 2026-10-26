package com.exponea.sdk.style

import android.util.TypedValue
import com.exponea.sdk.models.InAppMessageTest
import com.exponea.sdk.testutil.FakeDrawableCache
import com.exponea.sdk.testutil.FakeFontCache
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InAppImageStyleTest {

    @Test
    fun `should parse complete style`() {
        val uiPayloadBuilder = InAppRichstylePayloadBuilder(
            drawableCache = FakeDrawableCache(),
            fontCache = FakeFontCache()
        )
        val uiPayload = uiPayloadBuilder.build(InAppMessageTest.buildInAppMessageWithRichstyle().payload!!)
        val parsed = uiPayload!!.image.style
        assertEquals(ImageSizing.AUTO_HEIGHT, parsed.sizing)
        assertEquals(16, parsed.ratioWidth)
        assertEquals(9, parsed.ratioHeight)
        assertEquals(ImageScaling.FILL, parsed.scale)
        assertEquals(200, parsed.margin.top.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.top.unit)
        assertEquals(10, parsed.margin.right.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.right.unit)
        assertEquals(10, parsed.margin.bottom.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.bottom.unit)
        assertEquals(10, parsed.margin.left.toPx())
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.margin.left.unit)
    }
}
