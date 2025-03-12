package com.exponea.sdk.style

import android.graphics.Color
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.exponea.sdk.util.ConversionUtils
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ColorTest {

    @Test
    fun `should parse null value as null`() {
        assertNull(ConversionUtils.parseColor(null))
    }

    @Test
    fun `should parse named color`() {
        val parsed = ConversionUtils.parseColor("aliceblue")
        assertNotNull(parsed)
        // method `parseColor` is used from `android.graphics.Color`
        assertEquals(Color.parseColor("#f0f8ff"), parsed)
    }

    @Test
    fun `should parse HEX color`() {
        val parsed = ConversionUtils.parseColor("#7f11e0")
        assertNotNull(parsed)
        // method `parseColor` is used from `android.graphics.Color`
        assertEquals(127, parsed.red)
        assertEquals(17, parsed.green)
        assertEquals(224, parsed.blue)
        assertEquals(255, parsed.alpha)
    }

    @Test
    fun `should parse HEXA color`() {
        val parsed = ConversionUtils.parseColor("#7f11e0aa")
        assertNotNull(parsed)
        // method `parseColor` is used from `android.graphics.Color`
        assertEquals(127, parsed.red)
        assertEquals(17, parsed.green)
        assertEquals(224, parsed.blue)
        assertEquals(170, parsed.alpha)
    }

    @Test
    fun `should parse short HEX color`() {
        val parsed = ConversionUtils.parseColor("#ABC")
        assertNotNull(parsed)
        // method `parseColor` is used from `android.graphics.Color`
        assertEquals(170, parsed.red)
        assertEquals(187, parsed.green)
        assertEquals(204, parsed.blue)
        assertEquals(255, parsed.alpha)
    }

    @Test
    fun `should parse short HEXA color`() {
        val parsed = ConversionUtils.parseColor("#ABCD")
        assertNotNull(parsed)
        // method `parseColor` is used from `android.graphics.Color`
        assertEquals(170, parsed.red)
        assertEquals(187, parsed.green)
        assertEquals(204, parsed.blue)
        assertEquals(221, parsed.alpha)
    }

    @Test
    fun `should parse RGB color`() {
        val parsed = ConversionUtils.parseColor("rgb(127, 17, 224)")
        assertNotNull(parsed)
        // method `parseColor` is used from `android.graphics.Color`
        assertEquals(127, parsed.red)
        assertEquals(17, parsed.green)
        assertEquals(224, parsed.blue)
        assertEquals(255, parsed.alpha)
    }

    @Test
    fun `should parse RGBA color`() {
        val parsed = ConversionUtils.parseColor("rgba(127, 17, 224, 1)")
        assertNotNull(parsed)
        // method `parseColor` is used from `android.graphics.Color`
        assertEquals(127, parsed.red)
        assertEquals(17, parsed.green)
        assertEquals(224, parsed.blue)
        assertEquals(255, parsed.alpha)
    }
}
