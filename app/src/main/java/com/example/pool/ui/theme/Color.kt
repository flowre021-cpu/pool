package com.example.pool.ui.theme

import androidx.compose.runtime.Composable

/** 当前 Composable 树生效的主题调色板；由 [PoolTheme] 注入 */
val LocalPoolThemePalette = androidx.compose.runtime.staticCompositionLocalOf { PoolThemeCatalog.Default }

@Composable
fun poolThemePalette(): PoolThemePalette = LocalPoolThemePalette.current

@Composable
fun poolThemeColors(): PoolThemeColors = poolThemePalette().toThemeColors()

/**
 * 全局颜色访问入口。
 * - 黑/灰字、白底、删除红、DDL 紧迫色等 → [PoolFixedColors]（固定）
 * - 主强调 / 次强调 / 浅彩底 / FAB 等 → 随 [PoolThemeCatalog] 变化
 *
 * Widget 等非 Compose 代码在进程启动时同步一次 [applyTheme]。
 */
object PoolColors {
    private var themed: PoolThemeColors = PoolThemeCatalog.Default.toThemeColors()

    fun applyTheme(palette: PoolThemePalette) {
        themed = palette.toThemeColors()
    }

    val Background get() = PoolFixedColors.background
    val NavBar get() = PoolFixedColors.navBar
    val Block1 get() = PoolFixedColors.block1
    val Divider get() = PoolFixedColors.divider
    val TextPrimary get() = PoolFixedColors.textPrimary
    val TextSecondary get() = PoolFixedColors.textSecondary
    val DateHeader get() = themed.dateHeader
    val DeleteRed get() = PoolFixedColors.deleteRed

    val AccentPrimary get() = themed.accentPrimary
    val Accent get() = themed.accent
    val AccentDark get() = themed.accentDark
    val Block get() = themed.block
    val BlockDeep get() = themed.blockDeep
    val FabMint get() = themed.fabMint
    val DdlNormal get() = themed.ddlNormal

    val BackgroundDark get() = Background
    val NavBarDark get() = NavBar
    val TextPrimaryDark get() = TextPrimary
}

val DdlOverdue get() = PoolFixedColors.ddlOverdue
val DdlDueSoon get() = PoolFixedColors.ddlDueSoon
val DdlNormal get() = PoolColors.DdlNormal
