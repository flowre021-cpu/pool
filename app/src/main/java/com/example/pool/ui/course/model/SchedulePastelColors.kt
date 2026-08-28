package com.example.pool.ui.course.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 数据库 / HEX 存储的标准 ARGB Long → Compose Color。
 * 必须使用 [Color(Int)]（AARRGGBB），不可使用 [Color(ULong)] 打包格式。
 */
fun colorFromArgbLong(argb: Long): Color = Color((argb and 0xFFFFFFFFL).toInt())

/** Compose Color → 数据库存储 ARGB Long */
fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

fun colorToHex(color: Color): String = colorToDisplayHex(color)

/** UI 展示用：不透明时 #RRGGBB，含透明通道时 #AARRGGBB */
fun colorToDisplayHex(color: Color): String {
    val argb = color.toArgb()
    val alpha = (argb ushr 24) and 0xFF
    return if (alpha == 0xFF) {
        "#%06X".format(argb and 0xFFFFFF)
    } else {
        "#%08X".format(argb.toLong() and 0xFFFFFFFFL)
    }
}

fun parseHexColor(input: String): Color? {
    val cleaned = input.trim().removePrefix("#")
    val argbLong = when (cleaned.length) {
        6 -> ("FF$cleaned").toLongOrNull(16)
        8 -> cleaned.toLongOrNull(16)
        else -> null
    } ?: return null
    return colorFromArgbLong(argbLong)
}

fun colorFromHsv(h: Float, s: Float, v: Float): Color {
    val hsv = floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun colorToHsv(color: Color): FloatArray {
    val hsv = floatArrayOf(0f, 0f, 0f)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv
}

private fun argb(value: Long): Color = colorFromArgbLong(value)

/** 课表卡片配色与网格样式 */
object SchedulePastelColors {
    val default = argb(0xFFF0A8A8L)

    /** 马卡龙色系（适度提高饱和度，保留柔和质感） */
    val pastelPalette = listOf(
        argb(0xFFF0A8A8L), // 玫瑰粉
        argb(0xFF88D4BCL), // 薄荷绿
        argb(0xFFF0C898L), // 杏色
        argb(0xFF98B8E8L), // 矢车菊蓝
        argb(0xFFE0A8E0L), // 淡紫
        argb(0xFFE8E088L), // 柠檬黄
        argb(0xFFA8E0A8L), // 草绿
        argb(0xFFD0A8E8L), // 丁香紫
        argb(0xFF88C8E8L), // 天蓝
        argb(0xFFF0B898L), // 蜜桃
    )

    val palette = pastelPalette

    val cardText = Color(0xFF212121)
    val cardTextSecondary = Color(0xFF424242)
    val cardTextOnDark = Color.White
    val cardTextSecondaryOnDark = Color(0xFFE0E0E0)

    val gridLine = Color(0xFFF0F0F0)
    val swatchBorder = Color(0xFFD8D8D8)
    val gridHeaderBg = Color.White
    val sidebarBg = Color.White
    val formCardBg = Color(0xFFF7F7F7)
    val fabPastel = Color(0xFFB8C9A3)

    /** 根据卡片背景亮度选择可读性更高的文字颜色 */
    fun textOnCard(background: Color, secondary: Boolean = false): Color {
        val argb = background.toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return if (luminance > 0.62) {
            if (secondary) cardTextSecondary else cardText
        } else {
            if (secondary) cardTextSecondaryOnDark else cardTextOnDark
        }
    }
}
