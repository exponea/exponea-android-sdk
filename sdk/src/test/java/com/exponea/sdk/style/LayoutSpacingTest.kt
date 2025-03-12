package com.exponea.sdk.style

import android.util.TypedValue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LayoutSpacingTest {

    @Test
    fun `should parse null value as null`() {
        assertNull(LayoutSpacing.parse(null))
    }

    @Test
    fun `should parse integer value without type`() {
        val parsed = LayoutSpacing.parse("10")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.top.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.top.unit)
        assertEquals(10.0f, parsed.left.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.left.unit)
        assertEquals(10.0f, parsed.bottom.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.bottom.unit)
        assertEquals(10.0f, parsed.right.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.right.unit)
    }

    @Test
    fun `should parse single value - pixels`() {
        val parsed = LayoutSpacing.parse("10px")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.top.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.top.unit)
        assertEquals(10.0f, parsed.left.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.left.unit)
        assertEquals(10.0f, parsed.bottom.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.bottom.unit)
        assertEquals(10.0f, parsed.right.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.right.unit)
    }

    @Test
    fun `should parse two values - pixels`() {
        val parsed = LayoutSpacing.parse("10px 5px")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.top.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.top.unit)
        assertEquals(5.0f, parsed.left.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.left.unit)
        assertEquals(10.0f, parsed.bottom.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.bottom.unit)
        assertEquals(5.0f, parsed.right.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.right.unit)
    }

    @Test
    fun `should parse three values - pixels`() {
        val parsed = LayoutSpacing.parse("10px 5px 20px")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.top.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.top.unit)
        assertEquals(5.0f, parsed.left.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.left.unit)
        assertEquals(20.0f, parsed.bottom.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.bottom.unit)
        assertEquals(5.0f, parsed.right.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.right.unit)
    }

    @Test
    fun `should parse four values - pixels`() {
        val parsed = LayoutSpacing.parse("10px 5px 20px 30px")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.top.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.top.unit)
        assertEquals(30.0f, parsed.left.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.left.unit)
        assertEquals(20.0f, parsed.bottom.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.bottom.unit)
        assertEquals(5.0f, parsed.right.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.right.unit)
    }

    @Test
    fun `should not parse five values - pixels`() {
        val parsed = LayoutSpacing.parse("10px 5px 20px 30px 100px")
        assertNull(parsed)
    }

    @Test
    fun `should parse four values - mutual units`() {
        val parsed = LayoutSpacing.parse("10px 5mm 20pt 30in")
        assertNotNull(parsed)
        assertEquals(10.0f, parsed.top.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PX, parsed.top.unit)
        assertEquals(30.0f, parsed.left.size)
        assertEquals(TypedValue.COMPLEX_UNIT_IN, parsed.left.unit)
        assertEquals(20.0f, parsed.bottom.size)
        assertEquals(TypedValue.COMPLEX_UNIT_PT, parsed.bottom.unit)
        assertEquals(5.0f, parsed.right.size)
        assertEquals(TypedValue.COMPLEX_UNIT_MM, parsed.right.unit)
    }
}
