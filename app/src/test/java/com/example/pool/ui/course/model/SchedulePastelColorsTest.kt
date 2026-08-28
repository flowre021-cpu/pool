package com.example.pool.ui.course.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SchedulePastelColorsTest {
    @Test
    fun colorToDisplayHex_usesSixDigitsForOpaqueColor() {
        assertEquals("#F0A8A8", colorToDisplayHex(Color(0xFFF0A8A8)))
    }

    @Test
    fun colorToDisplayHex_usesEightDigitsForAlpha() {
        assertEquals("#80F0A8A8", colorToDisplayHex(Color(0x80F0A8A8)))
    }

    @Test
    fun parseHexColor_acceptsSixAndEightDigitInput() {
        assertEquals(Color(0xFF88D4BC), parseHexColor("#88D4BC"))
        assertEquals(Color(0x8088D4BC), parseHexColor("#8088D4BC"))
        assertNull(parseHexColor("#ZZZZZZ"))
    }
}
