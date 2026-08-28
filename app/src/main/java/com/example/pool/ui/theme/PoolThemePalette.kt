package com.example.pool.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * App 主题预设。固定字色/语义色见 [PoolFixedColors]；此处仅定义随预设切换的 accent 与浅彩表面。
 */
data class PoolThemePalette(
    val id: String,
    val displayName: String,
    val accent: PoolAccentColors,
    val surface: PoolThemedSurfaceColors,
    val semantic: PoolThemedSemanticColors,
)

data class PoolAccentColors(
    val primary: Color,
    val secondary: Color,
    val dark: Color,
)

/** 随主题变化的表面色（白底见 [PoolFixedColors]） */
data class PoolThemedSurfaceColors(
    val block: Color,
    val blockDeep: Color,
)

data class PoolThemedSemanticColors(
    val fab: Color,
    val ddlNormal: Color,
    val dateHeader: Color,
)

object PoolThemeCatalog {
    val Default: PoolThemePalette = PoolThemePalette(
        id = "default",
        displayName = "绿",
        accent = PoolAccentColors(
            primary = Color(0xFF248C3F),
            secondary = Color(0xFF729C44),
            dark = Color(0xFF5A7F35),
        ),
        surface = PoolThemedSurfaceColors(
            block = Color(0xFFF7FAF4),
            blockDeep = Color(0xFFDCE8CF),
        ),
        semantic = PoolThemedSemanticColors(
            fab = Color(0xFFB8E6CF),
            ddlNormal = Color(0xFF729C44),
            dateHeader = Color(0xFF678AB1),
        ),
    )

    val Blue: PoolThemePalette = PoolThemePalette(
        id = "blue",
        displayName = "蓝",
        accent = PoolAccentColors(
            primary = Color(0xFF2374AB),
            secondary = Color(0xFF5A7298),
            dark = Color(0xFF142864),
        ),
        surface = PoolThemedSurfaceColors(
            block = Color(0xFFF4F6FB),
            blockDeep = Color(0xFFD8DFF0),
        ),
        semantic = PoolThemedSemanticColors(
            fab = Color(0xFFB8C4E8),
            ddlNormal = Color(0xFF5A7298),
            dateHeader = Color(0xFFF4CAE0),
        ),
    )

    val Yellow: PoolThemePalette = PoolThemePalette(
        id = "yellow",
        displayName = "黄",
        accent = PoolAccentColors(
            primary = Color(0xFFF5D491),
            secondary = Color(0xFFC4B878),
            dark = Color(0xFFA89848),
        ),
        surface = PoolThemedSurfaceColors(
            block = Color(0xFFFDFCF5),
            blockDeep = Color(0xFFF0ECD8),
        ),
        semantic = PoolThemedSemanticColors(
            fab = Color(0xFFF9F0B8),
            ddlNormal = Color(0xFFB8A860),
            dateHeader = Color(0xFF826754),
        ),
    )

    val Pink: PoolThemePalette = PoolThemePalette(
        id = "pink",
        displayName = "粉",
        accent = PoolAccentColors(
            primary = Color(0xFFC47B7A),
            secondary = Color(0xFFD4A5A4),
            dark = Color(0xFFA86A69),
        ),
        surface = PoolThemedSurfaceColors(
            block = Color(0xFFFBF7F6),
            blockDeep = Color(0xFFEDE0DE),
        ),
        semantic = PoolThemedSemanticColors(
            fab = Color(0xFFE8D4D2),
            ddlNormal = Color(0xFFCF9897),
            dateHeader = Color(0xFF3A9D93),
        ),
    )

    val Orange: PoolThemePalette = PoolThemePalette(
        id = "orange",
        displayName = "橙",
        accent = PoolAccentColors(
            primary = Color(0xFFFFAD69),
            secondary = Color(0xFFD4A880),
            dark = Color(0xFFC48850),
        ),
        surface = PoolThemedSurfaceColors(
            block = Color(0xFFFBF8F5),
            blockDeep = Color(0xFFEDE4DC),
        ),
        semantic = PoolThemedSemanticColors(
            fab = Color(0xFFFFD8B8),
            ddlNormal = Color(0xFFD4A080),
            dateHeader = Color(0xFF393424),
        ),
    )

    val Purple: PoolThemePalette = PoolThemePalette(
        id = "purple",
        displayName = "紫",
        accent = PoolAccentColors(
            primary = Color(0xFFA882DD),
            secondary = Color(0xFFBBA0D4),
            dark = Color(0xFF8A6BB8),
        ),
        surface = PoolThemedSurfaceColors(
            block = Color(0xFFF9F7FC),
            blockDeep = Color(0xFFEBE4F5),
        ),
        semantic = PoolThemedSemanticColors(
            fab = Color(0xFFD9CCF0),
            ddlNormal = Color(0xFFBBA0D4),
            dateHeader = Color(0xFF2E4057),
        ),
    )

    val Gray: PoolThemePalette = PoolThemePalette(
        id = "gray",
        displayName = "灰",
        accent = PoolAccentColors(
            primary = Color(0xFF546E7A),
            secondary = Color(0xFF78909C),
            dark = Color(0xFF37474F),
        ),
        surface = PoolThemedSurfaceColors(
            block = Color(0xFFF7F9FA),
            blockDeep = Color(0xFFE0E4E7),
        ),
        semantic = PoolThemedSemanticColors(
            fab = Color(0xFFB0BEC5),
            ddlNormal = Color(0xFF78909C),
            dateHeader = Color(0xFF678AB1),
        ),
    )

    val all: List<PoolThemePalette> = listOf(
        Default,
        Blue,
        Yellow,
        Pink,
        Orange,
        Purple,
        Gray,
    )

    fun byId(id: String): PoolThemePalette =
        all.firstOrNull { it.id == id } ?: Default
}
