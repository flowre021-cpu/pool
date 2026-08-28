package com.example.pool.ui.home

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 时间轴布局：24 小时线性 vs 课表节次时间线 */
enum class AgendaTimeLayoutMode {
    HOUR_24,
    SECTION_TIMELINE,
}

fun AgendaTimeLayoutMode.next(): AgendaTimeLayoutMode = when (this) {
    AgendaTimeLayoutMode.HOUR_24 -> AgendaTimeLayoutMode.SECTION_TIMELINE
    AgendaTimeLayoutMode.SECTION_TIMELINE -> AgendaTimeLayoutMode.HOUR_24
}

fun AgendaTimeLayoutMode.contentDescription(): String = when (this) {
    AgendaTimeLayoutMode.HOUR_24 -> "24小时"
    AgendaTimeLayoutMode.SECTION_TIMELINE -> "节次"
}

/** 1天 / 3天 / 7天 视图档位 */
enum class AgendaZoomLevel(val visibleDays: Int) {
    ONE(1),
    THREE(3),
    WEEK(7),
}

fun AgendaZoomLevel.next(): AgendaZoomLevel = when (this) {
    AgendaZoomLevel.ONE -> AgendaZoomLevel.THREE
    AgendaZoomLevel.THREE -> AgendaZoomLevel.WEEK
    AgendaZoomLevel.WEEK -> AgendaZoomLevel.ONE
}

fun AgendaZoomLevel.contentDescription(): String = when (this) {
    AgendaZoomLevel.ONE -> "1天视图"
    AgendaZoomLevel.THREE -> "3天视图"
    AgendaZoomLevel.WEEK -> "7天视图"
}

/** 卡片信息密度：由实际像素宽度 + 高度 + 视图档位决定 */
enum class AgendaCardDensity {
    FULL,
    COMPACT,
    MINIMAL,
}

internal val AgendaOverlapGap = 2.dp

internal fun currentMinutesOfDay(): Int {
    val now = java.time.LocalTime.now()
    return now.hour * 60 + now.minute
}

/** 含秒的小数分钟，用于时间线精确定位 */
internal fun currentMinutesOfDayPrecise(): Float {
    val now = java.time.LocalTime.now()
    return now.hour * 60f + now.minute + now.second / 60f
}

internal fun visibleDayIndices(
    totalDays: Int,
    todayIndex: Int,
    zoomLevel: AgendaZoomLevel,
): List<Int> {
    if (totalDays <= 0) return emptyList()
    val safeTodayIndex = todayIndex.coerceIn(0, totalDays - 1)
    return when (zoomLevel) {
        AgendaZoomLevel.ONE -> listOf(safeTodayIndex)
        AgendaZoomLevel.THREE -> {
            val visibleDays = minOf(AgendaZoomLevel.THREE.visibleDays, totalDays)
            val start = centeredPageStart(safeTodayIndex, visibleDays, totalDays)
            (start until start + visibleDays).toList()
        }
        AgendaZoomLevel.WEEK -> (0 until totalDays).toList()
    }
}

internal fun resolveCardDensity(
    widthPx: Float,
    heightPx: Float,
    density: Density,
    zoomLevel: AgendaZoomLevel,
    overlapCount: Int,
): AgendaCardDensity {
    val width = with(density) { widthPx.toDp() }
    val height = with(density) { heightPx.toDp() }

    if (overlapCount > 1) {
        return when {
            width >= 52.dp && height >= 36.dp -> AgendaCardDensity.COMPACT
            else -> AgendaCardDensity.MINIMAL
        }
    }

    return when {
        height < 12.dp -> AgendaCardDensity.MINIMAL
        width >= 64.dp && height >= 28.dp -> AgendaCardDensity.FULL
        else -> AgendaCardDensity.COMPACT
    }
}

internal fun computeOverlapBounds(
    columnWidthPx: Float,
    column: Int,
    columnCount: Int,
    gapPx: Float,
): Pair<Float, Float> {
    if (columnCount <= 0) return 0f to columnWidthPx.coerceAtLeast(0f)
    val totalGap = gapPx * (columnCount + 1)
    val cardWidth = ((columnWidthPx - totalGap) / columnCount).coerceAtLeast(0f)
    val x = gapPx + column * (cardWidth + gapPx)
    return x to cardWidth
}

internal fun shouldShowEventTimeOnCard(
    zoomLevel: AgendaZoomLevel,
    block: AgendaGridBlock,
    cardHeightDp: Dp,
    cardWidthDp: Dp,
): Boolean {
    if (block !is AgendaGridBlock.Event) return false
    if (cardHeightDp < 16.dp || cardWidthDp < 18.dp) return false
    return when (zoomLevel) {
        AgendaZoomLevel.ONE, AgendaZoomLevel.THREE -> true
        AgendaZoomLevel.WEEK -> cardHeightDp >= 28.dp && cardWidthDp >= 24.dp
    }
}

internal enum class EventTimeLayout {
    HIDDEN,
    START_ONLY,
    STACKED,
    TWO_LINE,
    SINGLE_LINE,
}

internal fun resolveEventTimeLayout(
    widthDp: Dp,
    heightDp: Dp,
    zoomLevel: AgendaZoomLevel = AgendaZoomLevel.WEEK,
): EventTimeLayout {
    val contentW = widthDp - 12.dp
    val contentH = heightDp - 10.dp
    if (contentH < 12.dp || contentW < 14.dp) return EventTimeLayout.HIDDEN

    val preferMultiLine = zoomLevel != AgendaZoomLevel.WEEK && contentH < 44.dp

    return when {
        !preferMultiLine && contentW >= 60.dp && contentH >= 16.dp -> EventTimeLayout.SINGLE_LINE
        contentW >= 32.dp && contentH >= 22.dp -> EventTimeLayout.TWO_LINE
        contentH >= 28.dp -> EventTimeLayout.STACKED
        contentH >= 12.dp -> EventTimeLayout.START_ONLY
        else -> EventTimeLayout.HIDDEN
    }
}

internal fun pageCountForZoom(totalDays: Int, visibleDays: Int): Int =
    if (totalDays == 0) 1 else (totalDays + visibleDays - 1) / visibleDays

internal fun pageStartIndex(page: Int, visibleDays: Int): Int = page * visibleDays

internal fun todayPageIndex(todayIndex: Int, visibleDays: Int): Int =
    todayIndex / visibleDays

internal fun centeredPageStart(todayIndex: Int, visibleDays: Int, totalDays: Int): Int {
    if (visibleDays >= totalDays) return 0
    val half = visibleDays / 2
    return (todayIndex - half).coerceIn(0, totalDays - visibleDays)
}

internal fun pageForCenteredStart(startIndex: Int, visibleDays: Int): Int =
    startIndex / visibleDays
