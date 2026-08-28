package com.example.pool.ui.course

import com.example.pool.ui.course.model.colorFromArgbLong
import com.example.pool.ui.course.model.toArgbLong
import org.junit.Assert.assertEquals
import org.junit.Test

class CardColorPreferencesTest {
    @Test
    fun mergeCardPalette_prefersCustomThenBuiltInWithoutDuplicates() {
        val custom = listOf(0xFF112233L, 0xFF445566L).map(::colorFromArgbLong)
        val builtIn = listOf(0xFF445566L, 0xFF778899L).map(::colorFromArgbLong)
        val merged = mergeCardPalette(custom = custom, builtIn = builtIn)
        assertEquals(3, merged.size)
        assertEquals(0xFF112233L, merged[0].toArgbLong())
        assertEquals(0xFF445566L, merged[1].toArgbLong())
        assertEquals(0xFF778899L, merged[2].toArgbLong())
    }
}
