package com.example.pool.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 不随「主题色」切换而改变的 token：黑/灰字、白底、语义红橙、分割线等。
 * 课程/事件卡片色见 [com.example.pool.ui.course.model.CourseColorPalettes]。
 */
object PoolFixedColors {
    val textPrimary = Color(0xFF000000)
    val textSecondary = Color(0xFF666666)

    val background = Color(0xFFFFFFFF)
    val navBar = Color(0xFFFFFFFF)
    val block1 = Color(0xFFFFFFFF)

    val divider = Color(0xFFE0E0E0)

    val deleteRed = Color(0xFFD32F2F)
    val ddlOverdue = Color(0xFFE53935)
    val ddlDueSoon = Color(0xFFFB8C00)
}
