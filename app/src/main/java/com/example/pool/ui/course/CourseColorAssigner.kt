package com.example.pool.ui.course

import androidx.compose.ui.graphics.Color
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.ui.course.model.toArgbLong

object CourseColorAssigner {
    fun colorAtIndex(index: Int, palette: List<Color>): Color {
        if (palette.isEmpty()) return SchedulePastelColors.default
        return palette[index.mod(palette.size)]
    }

    fun assignColorsToGroups(
        groupIds: List<String>,
        palette: List<Color>,
    ): Map<String, Long> {
        if (palette.isEmpty()) return emptyMap()
        return groupIds.mapIndexed { index, groupId ->
            groupId to colorAtIndex(index, palette).toArgbLong()
        }.toMap()
    }
}
