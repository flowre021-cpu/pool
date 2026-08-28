package com.example.pool.ui.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import com.example.pool.ui.course.components.ScheduleSectionRowHeight
import com.example.pool.ui.course.model.TimeSlot

internal const val DAY_START_MINUTES = 0
internal const val DAY_END_MINUTES = 24 * 60
internal val AgendaHourHeight = 48.dp
internal val AgendaDayGridHeight = AgendaHourHeight * 24

internal fun agendaGridHeight(
    timeLayoutMode: AgendaTimeLayoutMode,
    timeSlots: List<TimeSlot>,
    sectionRowHeight: Dp = ScheduleSectionRowHeight,
): Dp = when {
    timeLayoutMode == AgendaTimeLayoutMode.SECTION_TIMELINE && timeSlots.isNotEmpty() ->
        sectionRowHeight * timeSlots.size
    else -> AgendaDayGridHeight
}

internal fun useSectionTimeGrid(
    timeLayoutMode: AgendaTimeLayoutMode,
    timeSlots: List<TimeSlot>,
): Boolean = timeLayoutMode == AgendaTimeLayoutMode.SECTION_TIMELINE && timeSlots.isNotEmpty()

/** 滚动对准事务内容垂直中心；无事务时默认 8:00–18:00 区间 */
internal fun agendaScrollCenterY(
    blocks: List<TimedLayoutEntry>,
    useSectionGrid: Boolean,
    timeSlots: List<TimeSlot>,
    sectionRowHeight: Dp = ScheduleSectionRowHeight,
): Dp {
    val centerMinutes = if (blocks.isNotEmpty()) {
        (blocks.minOf { it.startMinutes } + blocks.maxOf { it.endMinutes }) / 2f
    } else {
        (8 * 60 + 18 * 60) / 2f
    }
    return if (useSectionGrid && timeSlots.isNotEmpty()) {
        minutesToSectionYOffset(centerMinutes, timeSlots, sectionRowHeight)
    } else {
        minutesToYOffset(centerMinutes)
    }
}

internal fun agendaScrollTargetPx(
    centerY: Dp,
    viewportHeightPx: Float,
    gridHeightPx: Float,
    density: androidx.compose.ui.unit.Density,
): Int {
    val centerPx = with(density) { centerY.toPx() }
    val maxScroll = (gridHeightPx - viewportHeightPx).coerceAtLeast(0f)
    return (centerPx - viewportHeightPx / 2f).coerceIn(0f, maxScroll).toInt()
}

internal data class TimedLayoutEntry(
    val key: String,
    val startMinutes: Int,
    val endMinutes: Int,
)

internal data class PositionedLayoutEntry(
    val key: String,
    val column: Int,
    val columnCount: Int,
)

internal fun layoutOverlappingEntries(entries: List<TimedLayoutEntry>): List<PositionedLayoutEntry> {
    if (entries.isEmpty()) return emptyList()

    val clusters = buildOverlapClusters(entries)
    return clusters.flatMap { cluster -> layoutOverlapCluster(cluster) }
}

private fun buildOverlapClusters(entries: List<TimedLayoutEntry>): List<List<TimedLayoutEntry>> {
    val sorted = entries.sortedBy { it.startMinutes }
    val clusters = mutableListOf<MutableList<TimedLayoutEntry>>()

    for (entry in sorted) {
        val touched = clusters.filter { cluster -> cluster.any { overlaps(it, entry) } }
        when {
            touched.isEmpty() -> clusters.add(mutableListOf(entry))
            touched.size == 1 -> touched[0].add(entry)
            else -> {
                val merged = touched.flatMap { it }.toMutableList()
                merged.add(entry)
                touched.forEach { clusters.remove(it) }
                clusters.add(merged)
            }
        }
    }
    return clusters
}

private fun layoutOverlapCluster(cluster: List<TimedLayoutEntry>): List<PositionedLayoutEntry> {
    val sorted = cluster.sortedWith(
        compareBy<TimedLayoutEntry>({ it.startMinutes }).thenByDescending { it.endMinutes - it.startMinutes },
    )
    val columnEnds = mutableListOf<Int>()
    val assignments = linkedMapOf<String, Int>()

    for (entry in sorted) {
        var column = columnEnds.indexOfFirst { it <= entry.startMinutes }
        if (column == -1) {
            column = columnEnds.size
            columnEnds.add(entry.endMinutes)
        } else {
            columnEnds[column] = entry.endMinutes
        }
        assignments[entry.key] = column
    }

    val columnCount = (assignments.values.maxOrNull() ?: 0) + 1
    return assignments.map { (key, column) ->
        PositionedLayoutEntry(
            key = key,
            column = column,
            columnCount = columnCount,
        )
    }
}

private fun overlaps(a: TimedLayoutEntry, b: TimedLayoutEntry): Boolean =
    a.startMinutes < b.endMinutes && b.startMinutes < a.endMinutes

internal fun minutesToYOffset(
    minutes: Float,
    hourHeight: Dp = AgendaHourHeight,
): Dp {
    val clamped = minutes.coerceIn(DAY_START_MINUTES.toFloat(), DAY_END_MINUTES.toFloat())
    return hourHeight * (clamped / 60f)
}

internal fun blockGeometry(
    startMinutes: Int,
    endMinutes: Int,
    hourHeight: Dp = AgendaHourHeight,
    minHeight: Dp = 28.dp,
): Pair<Dp, Dp> {
    val top = minutesToYOffset(startMinutes.toFloat(), hourHeight)
    val bottom = minutesToYOffset(
        endMinutes.coerceIn(DAY_START_MINUTES + 1, DAY_END_MINUTES).toFloat(),
        hourHeight,
    )
    val height = (bottom - top).coerceAtLeast(minHeight)
    return top to height
}

/** 节次网格：在首末节次时间范围内线性映射，课间空白也能准确定位 */
internal fun blockGeometryWithSections(
    startMinutes: Int,
    endMinutes: Int,
    slots: List<TimeSlot>,
    rowHeight: Dp,
    minHeight: Dp = 28.dp,
): Pair<Dp, Dp> {
    if (slots.isEmpty()) return blockGeometry(startMinutes, endMinutes, minHeight = minHeight)
    val top = minutesToSectionYOffset(startMinutes.toFloat(), slots, rowHeight)
    val bottom = minutesToSectionYOffset(
        endMinutes.coerceAtLeast(startMinutes + 1).toFloat(),
        slots,
        rowHeight,
    )
    val height = (bottom - top).coerceAtLeast(minHeight)
    return top to height
}

internal fun minutesToSectionYOffset(
    minutes: Float,
    slots: List<TimeSlot>,
    rowHeight: Dp,
): Dp {
    if (slots.isEmpty()) return minutesToYOffset(minutes)
    val first = slots.first()
    val last = slots.last()
    val spanStart = first.startTimeMinutes.toFloat()
    val spanEnd = last.endTimeMinutes.toFloat()
    val totalSpan = (spanEnd - spanStart).coerceAtLeast(1f)
    val clamped = minutes.coerceIn(spanStart, spanEnd)
    val fraction = (clamped - spanStart) / totalSpan
    return rowHeight * slots.size * fraction
}

internal fun findTimeLabelRightOffsetPx(
    nowYpx: Float,
    obstructions: List<TimeLineObstruction>,
    columnWidthPx: Float,
    labelWidthPx: Float,
    paddingPx: Float = 2f,
): Float {
    if (columnWidthPx <= 0f) return paddingPx
    val lineHalfHeight = 6f
    val lineTop = nowYpx - lineHalfHeight
    val lineBottom = nowYpx + lineHalfHeight

    val overlapping = obstructions.filter { obs ->
        obs.topPx < lineBottom && obs.bottomPx > lineTop
    }

    val occupied = overlapping
        .map { it.leftPx to it.rightPx }
        .sortedBy { it.first }
        .fold(mutableListOf<Pair<Float, Float>>()) { merged, range ->
            if (merged.isEmpty() || range.first > merged.last().second + paddingPx) {
                merged.add(range.first to range.second)
            } else {
                val last = merged.removeAt(merged.lastIndex)
                merged.add(last.first to maxOf(last.second, range.second))
            }
            merged
        }

    val gaps = mutableListOf<Pair<Float, Float>>()
    var cursor = paddingPx
    for ((left, right) in occupied) {
        if (left - cursor >= labelWidthPx) {
            gaps.add(cursor to left - paddingPx)
        }
        cursor = maxOf(cursor, right + paddingPx)
    }
    if (columnWidthPx - cursor >= labelWidthPx) {
        gaps.add(cursor to columnWidthPx - paddingPx)
    }

    val fallback = (columnWidthPx - labelWidthPx - paddingPx).coerceAtLeast(paddingPx)
    if (gaps.isEmpty()) return fallback

    // 优先右侧空白：选最靠右且能容纳标注的区间，标注右对齐
    val rightGap = gaps
        .filter { it.second - it.first >= labelWidthPx }
        .maxByOrNull { it.second }
        ?: gaps.maxByOrNull { it.second - it.first }
        ?: return fallback

    return (rightGap.second - labelWidthPx).coerceIn(
        rightGap.first,
        (columnWidthPx - labelWidthPx - paddingPx).coerceAtLeast(paddingPx),
    )
}

internal data class TimeLineObstruction(
    val leftPx: Float,
    val rightPx: Float,
    val topPx: Float,
    val bottomPx: Float,
)

internal fun clipToDayMinutes(
    startAt: Long,
    endAt: Long?,
    date: java.time.LocalDate,
    zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
): Pair<Int, Int> {
    val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val effectiveStart = maxOf(startAt, dayStart)
    val effectiveEnd = minOf(endAt ?: (startAt + 60 * 60 * 1000), dayEnd)
    if (effectiveStart >= effectiveEnd) {
        return DAY_END_MINUTES to DAY_END_MINUTES
    }
    val startMinutes = if (effectiveStart >= dayEnd) {
        DAY_END_MINUTES
    } else {
        millisToMinutesOfDay(effectiveStart, zone)
    }
    val endMinutes = if (effectiveEnd >= dayEnd) {
        DAY_END_MINUTES
    } else {
        millisToMinutesOfDay(effectiveEnd, zone)
    }.coerceAtLeast(startMinutes + 1)
    return startMinutes to endMinutes.coerceAtMost(DAY_END_MINUTES)
}

private fun millisToMinutesOfDay(millis: Long, zone: java.time.ZoneId): Int {
    val time = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
    return time.hour * 60 + time.minute
}

internal fun nowYOffset(
    useSectionGrid: Boolean,
    timeSlots: List<TimeSlot>,
    sectionRowHeight: Dp,
): Dp? {
    val minutes = currentMinutesOfDayPrecise()
    return if (useSectionGrid && timeSlots.isNotEmpty()) {
        val firstStart = timeSlots.first().startTimeMinutes.toFloat()
        val lastEnd = timeSlots.last().endTimeMinutes.toFloat()
        if (minutes !in firstStart..lastEnd) {
            return null
        }
        minutesToSectionYOffset(minutes, timeSlots, sectionRowHeight)
    } else {
        minutesToYOffset(minutes)
    }
}
