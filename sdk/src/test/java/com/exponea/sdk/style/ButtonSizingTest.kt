package com.exponea.sdk.style

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ButtonSizingTest {

    @Test
    fun `parse should return HUG_TEXT for hug_text value`() {
        assertEquals(ButtonSizing.HUG_TEXT, ButtonSizing.parse("hug_text"))
    }

    @Test
    fun `parse should return FILL for fill value`() {
        assertEquals(ButtonSizing.FILL, ButtonSizing.parse("fill"))
    }

    @Test
    fun `parse should return null for null input`() {
        assertNull(ButtonSizing.parse(null))
    }

    @Test
    fun `parse should return null for empty string`() {
        assertNull(ButtonSizing.parse(""))
    }

    @Test
    fun `parse should return null for unknown value`() {
        assertNull(ButtonSizing.parse("hug"))
    }
}
