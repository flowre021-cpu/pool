package com.example.pool.ui.course.model

import androidx.compose.ui.graphics.Color

/** 课程卡片默认色卡（用户提供的全部预设色，只读） */
object CourseColorPalettes {
    const val MAX_CUSTOM_COLORS = 20

    val defaultColors: List<Color> = listOf(
        // 色卡 1
        hex(0xFFC2B2B4),
        hex(0xFF6B4E71),
        hex(0xFF3A4454),
        hex(0xFF53687E),
        hex(0xFFF5DDDD),
        // 色卡 2
        hex(0xFFF2545B),
        hex(0xFF13505B),
        hex(0xFF5C946E),
        hex(0xFF80C2AF),
        hex(0xFFA0DDE6),
        // 色卡 3
        hex(0xFFFFE8D1),
        hex(0xFFB38CB4),
        hex(0xFFA30B37),
        hex(0xFF2E294E),
        hex(0xFF70ABAF),
        // 色卡 5
        hex(0xFFD4E6B5),
        hex(0xFFAFC97E),
        hex(0xFFE2D686),
        hex(0xFFFFDF64),
        hex(0xFF877B66),
    )

    private fun hex(argb: Long): Color = colorFromArgbLong(argb)
}
