package com.example.pool.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppIconCatalogTest {
    @Test
    fun byId_returnsDefaultForUnknown() {
        assertEquals(AppIconCatalog.Pond, AppIconCatalog.byId("missing"))
    }

    @Test
    fun defaultIcon_isPond() {
        assertEquals(AppIconCatalog.POND_ID, AppIconCatalog.Default.id)
    }
}
