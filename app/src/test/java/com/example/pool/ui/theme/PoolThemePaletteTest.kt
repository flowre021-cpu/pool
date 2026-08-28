package com.example.pool.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PoolThemePaletteTest {
    @Test
    fun byId_returnsDefaultForUnknown() {
        assertEquals(PoolThemeCatalog.Default, PoolThemeCatalog.byId("missing"))
    }

    @Test
    fun blueTheme_differsFromDefaultAccent() {
        val blue = PoolThemeCatalog.byId("blue")
        assertNotEquals(
            PoolThemeCatalog.Default.accent.primary,
            blue.accent.primary,
        )
    }

    @Test
    fun toThemeColors_mapsAccentPrimary() {
        val colors = PoolThemeCatalog.Default.toThemeColors()
        assertEquals(PoolThemeCatalog.Default.accent.primary, colors.accentPrimary)
    }
}
