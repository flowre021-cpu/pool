package com.example.pool.ui.course

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pool.ui.theme.PoolColors

internal object ScheduleFormStyles {
    val sectionTitle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = PoolColors.TextPrimary,
    )

    val body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = PoolColors.TextPrimary,
    )

    val secondary = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = PoolColors.TextSecondary,
    )

    val input = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = PoolColors.TextPrimary,
    )

    val placeholder = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF9E9E9E),
    )

    val cardBackground = Color(0xFFF5F5F5)
}
