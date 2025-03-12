package com.exponea.sdk.util

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversionUtilsTest {
    @Test
    fun `should parse number correctly`() {
        // Integer
        assertEquals(10, ConversionUtils.parseNumber(10)?.toInt())
        assertEquals(10, ConversionUtils.parseNumber("10")?.toInt())
        assertEquals(-10, ConversionUtils.parseNumber(-10)?.toInt())
        assertEquals(-10, ConversionUtils.parseNumber("-10")?.toInt())
        // Double
        assertEquals(10.5, ConversionUtils.parseNumber(10.5)?.toDouble())
        assertEquals(10.5, ConversionUtils.parseNumber("10.5")?.toDouble())
        assertEquals(-10.5, ConversionUtils.parseNumber(-10.5)?.toDouble())
        assertEquals(-10.5, ConversionUtils.parseNumber("-10.5")?.toDouble())
        // Float
        assertEquals(10.5f, ConversionUtils.parseNumber(10.5f)?.toFloat())
        assertEquals(10.5f, ConversionUtils.parseNumber("10.5")?.toFloat())
        assertEquals(-10.5f, ConversionUtils.parseNumber(-10.5f)?.toFloat())
        assertEquals(-10.5f, ConversionUtils.parseNumber("-10.5")?.toFloat())
        // Invalid
        assertNull(ConversionUtils.parseNumber("hello"))
        assertNull(ConversionUtils.parseNumber(null))
        assertNull(ConversionUtils.parseNumber(true))
        assertNull(ConversionUtils.parseNumber(arrayOf<String>()))
    }
}
