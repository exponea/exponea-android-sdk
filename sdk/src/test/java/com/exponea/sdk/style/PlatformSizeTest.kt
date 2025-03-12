package com.exponea.sdk.style

import android.util.TypedValue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PlatformSizeTest {

    @Test
    fun `should parse null value as null`() {
        assertNull(PlatformSize.parse(null))
    }

    @Test
    fun `should parse integer value without type`() {
        val parsed = PlatformSize.parse("10")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.unit)
    }

    @Test
    fun `should parse decimal value without type`() {
        val parsed = PlatformSize.parse("10.4")
        assertNotNull(parsed)
        assertEquals(10.4f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.unit)
    }

    @Test
    fun `should parse negative decimal value without type`() {
        val parsed = PlatformSize.parse("-10.4")
        assertNotNull(parsed)
        assertEquals(-10.4f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.unit)
    }

    @Test
    fun `should parse negative integer value without type`() {
        val parsed = PlatformSize.parse("-10")
        assertNotNull(parsed)
        assertEquals(-10f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.unit)
    }

    @Test
    fun `should parse value with pixels`() {
        val parsed = PlatformSize.parse("10px")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.unit)
    }

    @Test
    fun `should parse value with inches`() {
        val parsed = PlatformSize.parse("10in")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_IN, parsed.unit)
    }

    @Test
    fun `should parse value with millimeters`() {
        val parsed = PlatformSize.parse("10mm")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_MM, parsed.unit)
    }

    @Test
    fun `should parse value with points`() {
        val parsed = PlatformSize.parse("10pt")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PT, parsed.unit)
    }

    @Test
    fun `should parse value with independent pixels`() {
        val parsed = PlatformSize.parse("10dp")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.unit)
    }

    @Test
    fun `should parse value with long independent pixels`() {
        val parsed = PlatformSize.parse("10dip")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_DIP, parsed.unit)
    }

    @Test
    fun `should parse value with scaled pixels`() {
        val parsed = PlatformSize.parse("10sp")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.size)
        assertEquals(TypedValue.COMPLEX_UNIT_SP, parsed.unit)
    }

    @Test
    fun `should convert from px to px`() {
        val parsed = PlatformSize(
            unit = TypedValue.COMPLEX_UNIT_PX,
            size = 10.0f
        )
        assertEquals(10, parsed.toPx())
    }

    @Test
    fun `should convert from in to px`() {
        val parsed = PlatformSize(
            unit = TypedValue.COMPLEX_UNIT_IN,
            size = 10.0f
        )
        assertEquals(1600, parsed.toPx())
    }

    @Test
    fun `should convert from mm to px`() {
        val parsed = PlatformSize(
            unit = TypedValue.COMPLEX_UNIT_MM,
            size = 10.0f
        )
        assertEquals(63, parsed.toPx())
    }

    @Test
    fun `should convert from pt to px`() {
        val parsed = PlatformSize(
            unit = TypedValue.COMPLEX_UNIT_PT,
            size = 10.0f
        )
        assertEquals(22, parsed.toPx())
    }

    @Test
    fun `should convert from dp to px`() {
        val parsed = PlatformSize(
            unit = TypedValue.COMPLEX_UNIT_DIP,
            size = 10.0f
        )
        assertEquals(10, parsed.toPx())
    }

    @Test
    fun `should convert from sp to px`() {
        val parsed = PlatformSize(
            unit = TypedValue.COMPLEX_UNIT_SP,
            size = 10.0f
        )
        assertEquals(10, parsed.toPx())
    }
}
