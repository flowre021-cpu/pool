package com.example.pool.ui.course

import com.example.pool.ui.course.model.CourseColorPalettes
import com.example.pool.ui.course.model.toArgbLong
import org.junit.Assert.assertEquals
import org.junit.Test

class CourseColorAssignerTest {
    @Test
    fun assignColorsToGroups_mapsOneToOneThenWraps() {
        val palette = CourseColorPalettes.defaultColors
        val groups = listOf("a", "b", "c")
        val result = CourseColorAssigner.assignColorsToGroups(groups, palette)
        assertEquals(palette[0].toArgbLong(), result["a"])
        assertEquals(palette[2].toArgbLong(), result["c"])
    }

    @Test
    fun defaultPalette_hasTwentyColors() {
        assertEquals(20, CourseColorPalettes.defaultColors.size)
    }
}
