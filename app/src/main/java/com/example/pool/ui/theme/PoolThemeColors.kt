package com.example.pool.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 随「主题色」预设（[PoolThemeCatalog]）切换的彩色 token。
 * Compose 内优先 [poolThemeColors]；非 Compose（Widget）在应用启动时 [PoolColors.applyTheme]。
 */
data class PoolThemeColors(
    val accentPrimary: Color,
    val accent: Color,
    val accentDark: Color,
    val block: Color,
    val blockDeep: Color,
    val fabMint: Color,
    /** 未过期 DDL 文字色，随主题 accent 系变化 */
    val ddlNormal: Color,
    /** 日期标题等辅助色：与主强调色异色系、降低视觉疲劳 */
    val dateHeader: Color,
)

fun PoolThemePalette.toThemeColors(): PoolThemeColors = PoolThemeColors(
    accentPrimary = accent.primary,
    accent = accent.secondary,
    accentDark = accent.dark,
    block = surface.block,
    blockDeep = surface.blockDeep,
    fabMint = semantic.fab,
    ddlNormal = semantic.ddlNormal,
    dateHeader = semantic.dateHeader,
)
