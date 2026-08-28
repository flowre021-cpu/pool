package com.example.pool.ui.home

import androidx.compose.ui.graphics.Color
import com.example.pool.data.AffairEntity
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.ui.course.model.colorFromArgbLong
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.DeadlineUrgency
import com.example.pool.util.ReminderRecurrenceMode
import com.example.pool.util.deadlineColor
import com.example.pool.util.deadlineUrgency
import com.example.pool.util.isReminderOccurrenceCompleted
import com.example.pool.util.parseReminderRecurrence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val TIMELINE_DAY_COUNT = 14
/** Opportunity 竖线最多分配泳道数，超出部分叠在最末泳道 */
const val MAX_OPPORTUNITY_LANES = 12
/** 提醒圆点固定在第 0 泳道 x 处，避免 opportunity 过多时被挤出屏幕 */
const val TIMELINE_REMINDER_LANE_INDEX = 0

data class TimelineUiState(
    val windowStart: LocalDate = LocalDate.now(),
    val days: List<TimelineDayState> = emptyList(),
    val opportunitySegments: List<TimelineOpportunitySegment> = emptyList(),
    val taskMarkers: List<TimelineTaskMarker> = emptyList(),
    val reminderMarkers: List<TimelineReminderMarker> = emptyList(),
    val dayDetailsByDate: Map<LocalDate, TimelineDayDetail> = emptyMap(),
)

data class TimelineDayState(
    val date: LocalDate,
    val dayIndex: Int,
    val isToday: Boolean,
    val taskCount: Int,
    val opportunityCount: Int,
    val reminderCount: Int,
)

data class TimelineOpportunitySegment(
    val id: Long,
    val title: String,
    val startDayIndex: Int,
    val endDayIndex: Int,
    val laneIndex: Int = 0,
    val color: Color,
)

data class TimelineTaskMarker(
    val id: Long,
    val title: String,
    val dayIndex: Int,
    /** 0–1，对应一天内的垂直位置 */
    val timeFraction: Float,
    val color: Color,
    /** 同刻分组内的序号（0..groupSize-1） */
    val offsetIndex: Int = 0,
    val groupSize: Int = 1,
)

data class TimelineReminderMarker(
    val id: Long,
    val title: String,
    val dayIndex: Int,
    val timeFraction: Float,
    val offsetIndex: Int = 0,
    val groupSize: Int = 1,
)

data class TimelineDayDetail(
    val date: LocalDate,
    val tasks: List<AffairEntity>,
    val opportunities: List<AffairEntity>,
    val reminders: List<AffairEntity>,
)

fun buildTimelineUiState(
    tasks: List<AffairEntity>,
    opportunities: List<AffairEntity>,
    reminders: List<AffairEntity>,
    showReminders: Boolean,
    today: LocalDate = LocalDate.now(),
): TimelineUiState {
    val windowStart = today
    val days = (0 until TIMELINE_DAY_COUNT).map { offset ->
        windowStart.plusDays(offset.toLong())
    }
    val dayIndexByDate = days.withIndex().associate { (index, date) -> date to index }

    val taskMarkers = staggerTimelineTaskMarkers(
        tasks.mapNotNull { task ->
            if (task.isDone) return@mapNotNull null
            val deadline = task.deadline ?: return@mapNotNull null
            val date = millisToLocalDate(deadline)
            val dayIndex = dayIndexByDate[date] ?: return@mapNotNull null
            TimelineTaskMarker(
                id = task.id,
                title = task.title,
                dayIndex = dayIndex,
                timeFraction = timeFractionOfDay(deadline),
                color = taskMarkerColor(task),
            )
        },
    )

    val opportunitySegments = assignOpportunityLanes(
        opportunities.mapNotNull { opportunity ->
            if (opportunity.isDone) return@mapNotNull null
            val range = opportunityDateRange(opportunity.startAt, opportunity.endAt) ?: return@mapNotNull null
            val clipped = clipRangeToWindow(range, days) ?: return@mapNotNull null
            TimelineOpportunitySegment(
                id = opportunity.id,
                title = opportunity.title,
                startDayIndex = clipped.first,
                endDayIndex = clipped.second,
                color = opportunityColor(opportunity),
            )
        },
    )

    val reminderMarkers = if (!showReminders) {
        emptyList()
    } else {
        staggerTimelineReminderMarkers(
            reminders.flatMap { reminder ->
                if (reminder.isDone) return@flatMap emptyList<TimelineReminderMarker>()
                reminderOccurrencesInWindow(reminder, days, today).mapNotNull { date ->
                    val dayIndex = dayIndexByDate[date] ?: return@mapNotNull null
                    TimelineReminderMarker(
                        id = reminder.id,
                        title = reminder.title,
                        dayIndex = dayIndex,
                        timeFraction = reminder.startAt?.let { timeFractionOfDay(it) } ?: 0.12f,
                    )
                }.distinctBy { "${it.id}-${it.dayIndex}" }
            },
        )
    }

    val dayDetails = buildDayAffairIndex(tasks, opportunities, reminders, days, showReminders, today)
    val dayDetailsByDate = days.associate { date ->
        val buckets = dayDetails[date] ?: DayAffairBuckets.EMPTY
        date to TimelineDayDetail(
            date = date,
            tasks = buckets.tasks,
            opportunities = buckets.opportunities,
            reminders = buckets.reminders,
        )
    }
    val timelineDays = days.mapIndexed { index, date ->
        val detail = dayDetails[date] ?: DayAffairBuckets.EMPTY
        TimelineDayState(
            date = date,
            dayIndex = index,
            isToday = date == today,
            taskCount = detail.tasks.size,
            opportunityCount = detail.opportunities.size,
            reminderCount = detail.reminders.size,
        )
    }

    return TimelineUiState(
        windowStart = windowStart,
        days = timelineDays,
        opportunitySegments = opportunitySegments,
        taskMarkers = taskMarkers,
        reminderMarkers = reminderMarkers,
        dayDetailsByDate = dayDetailsByDate,
    )
}

fun buildTimelineDayDetail(
    date: LocalDate,
    tasks: List<AffairEntity>,
    opportunities: List<AffairEntity>,
    reminders: List<AffairEntity>,
    showReminders: Boolean,
    today: LocalDate = LocalDate.now(),
): TimelineDayDetail {
    val index = buildDayAffairIndex(
        tasks = tasks,
        opportunities = opportunities,
        reminders = reminders,
        days = listOf(date),
        showReminders = showReminders,
        today = today,
    )
    val buckets = index[date] ?: DayAffairBuckets.EMPTY
    return TimelineDayDetail(
        date = date,
        tasks = buckets.tasks,
        opportunities = buckets.opportunities,
        reminders = buckets.reminders,
    )
}

private data class DayAffairBuckets(
    val tasks: List<AffairEntity>,
    val opportunities: List<AffairEntity>,
    val reminders: List<AffairEntity>,
) {
    companion object {
        val EMPTY = DayAffairBuckets(emptyList(), emptyList(), emptyList())
    }
}

private fun buildDayAffairIndex(
    tasks: List<AffairEntity>,
    opportunities: List<AffairEntity>,
    reminders: List<AffairEntity>,
    days: List<LocalDate>,
    showReminders: Boolean,
    today: LocalDate,
): Map<LocalDate, DayAffairBuckets> {
    val windowSet = days.toSet()
    val result = days.associateWith { DayAffairBuckets.EMPTY }.toMutableMap()

    tasks.forEach { task ->
        if (task.isDone) return@forEach
        val deadline = task.deadline ?: return@forEach
        val date = millisToLocalDate(deadline)
        if (date !in windowSet) return@forEach
        val current = result.getValue(date)
        result[date] = current.copy(tasks = current.tasks + task)
    }

    opportunities.forEach { opportunity ->
        if (opportunity.isDone) return@forEach
        val range = opportunityDateRange(opportunity.startAt, opportunity.endAt) ?: return@forEach
        days.filter { it in range.first..range.second }.forEach { date ->
            val current = result.getValue(date)
            if (opportunity !in current.opportunities) {
                result[date] = current.copy(opportunities = current.opportunities + opportunity)
            }
        }
    }

    if (showReminders) {
        reminders.forEach { reminder ->
            reminderOccurrencesInWindow(reminder, days, today).forEach { date ->
                val current = result.getValue(date)
                if (reminder !in current.reminders) {
                    result[date] = current.copy(reminders = current.reminders + reminder)
                }
            }
        }
    }

    return result
}

internal fun millisToLocalDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

internal fun opportunityDateRange(startAt: Long?, endAt: Long?): Pair<LocalDate, LocalDate>? {
    if (startAt == null && endAt == null) return null
    val start = startAt?.let { millisToLocalDate(it) }
        ?: endAt?.let { millisToLocalDate(it) }
        ?: return null
    val end = endAt?.let { millisToLocalDate(it) } ?: start
    return if (start <= end) start to end else end to start
}

internal fun clipRangeToWindow(
    range: Pair<LocalDate, LocalDate>,
    window: List<LocalDate>,
): Pair<Int, Int>? {
    if (window.isEmpty()) return null
    val winStart = window.first()
    val winEnd = window.last()
    val clippedStart = maxOf(range.first, winStart)
    val clippedEnd = minOf(range.second, winEnd)
    if (clippedStart > clippedEnd) return null
    val startIdx = window.indexOf(clippedStart)
    val endIdx = window.indexOf(clippedEnd)
    if (startIdx < 0 || endIdx < 0) return null
    return startIdx to endIdx
}

internal fun reminderOccurrencesInWindow(
    reminder: AffairEntity,
    window: List<LocalDate>,
    today: LocalDate = LocalDate.now(),
): List<LocalDate> {
    if (window.isEmpty()) return emptyList()
    val recurrence = parseReminderRecurrence(reminder.recurrenceRule)
    return when (recurrence.mode) {
        ReminderRecurrenceMode.DAILY -> window
        ReminderRecurrenceMode.WEEKLY -> {
            if (recurrence.weeklyDays.isEmpty()) emptyList()
            else window.filter { it.dayOfWeek.value in recurrence.weeklyDays }
        }
        ReminderRecurrenceMode.NONE -> {
            reminder.startAt?.let { millisToLocalDate(it) }
                ?.takeIf { it in window.first()..window.last() }
                ?.let { listOf(it) }
                ?: if (reminder.startAt == null && reminder.recurrenceRule.isNullOrBlank() && today in window.first()..window.last()) {
                    listOf(today)
                } else {
                    emptyList()
                }
        }
    }.filter { !isReminderOccurrenceCompleted(reminder, it) }
}

private fun taskMarkerColor(task: AffairEntity): Color {
    if (task.isDone) return PoolColors.TextSecondary.copy(alpha = 0.45f)
    return deadlineColor(deadlineUrgency(task.deadline, task.isDone))
}

private fun opportunityColor(opportunity: AffairEntity): Color {
    opportunity.cardColor?.let { return colorFromArgbLong(it) }
    val palette = SchedulePastelColors.pastelPalette
    if (palette.isEmpty()) return PoolColors.Accent
    return palette[(opportunity.id % palette.size).toInt()]
}

/** 一天内的相对位置；仅日期时默认靠近当天末尾（DDL 常见） */
internal fun timeFractionOfDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Float {
    val zdt = Instant.ofEpochMilli(millis).atZone(zone)
    val minuteOfDay = zdt.hour * 60 + zdt.minute
    if (minuteOfDay == 0 && zdt.second == 0) return 0.88f
    return (minuteOfDay / (24f * 60f)).coerceIn(0.06f, 0.94f)
}

/** 同 day + 同 timeFraction 的标记分配轻微错位序号，稳定按 id 排序 */
internal fun staggerTimelineTaskMarkers(
    markers: List<TimelineTaskMarker>,
): List<TimelineTaskMarker> = staggerTimelineMarkers(
    markers = markers,
    groupKey = { it.dayIndex to it.timeFraction },
    idKey = { it.id },
) { marker, offsetIndex, groupSize ->
    marker.copy(offsetIndex = offsetIndex, groupSize = groupSize)
}

internal fun staggerTimelineReminderMarkers(
    markers: List<TimelineReminderMarker>,
): List<TimelineReminderMarker> = staggerTimelineMarkers(
    markers = markers,
    groupKey = { it.dayIndex to it.timeFraction },
    idKey = { it.id },
) { marker, offsetIndex, groupSize ->
    marker.copy(offsetIndex = offsetIndex, groupSize = groupSize)
}

private fun <T> staggerTimelineMarkers(
    markers: List<T>,
    groupKey: (T) -> Pair<Int, Float>,
    idKey: (T) -> Long,
    copyWithOffset: (T, offsetIndex: Int, groupSize: Int) -> T,
): List<T> {
    if (markers.isEmpty()) return emptyList()
    return markers
        .groupBy(groupKey)
        .values
        .flatMap { group ->
            val sorted = group.sortedBy(idKey)
            val size = sorted.size
            sorted.mapIndexed { index, marker ->
                copyWithOffset(marker, index, size)
            }
        }
}

/** 重叠 opportunity 分配到不同竖向泳道，线段绘制在轴右侧 */
internal fun assignOpportunityLanes(
    segments: List<TimelineOpportunitySegment>,
): List<TimelineOpportunitySegment> {
    if (segments.isEmpty()) return emptyList()
    val sorted = segments.sortedWith(
        compareBy<TimelineOpportunitySegment>({ it.startDayIndex }).thenByDescending {
            it.endDayIndex - it.startDayIndex
        },
    )
    val laneEnds = mutableListOf<Int>()
    return sorted.map { segment ->
        var lane = laneEnds.indexOfFirst { endDay -> endDay < segment.startDayIndex }
        if (lane == -1) {
            lane = if (laneEnds.size < MAX_OPPORTUNITY_LANES) {
                laneEnds.size
            } else {
                MAX_OPPORTUNITY_LANES - 1
            }
            if (lane == laneEnds.size) {
                laneEnds.add(segment.endDayIndex)
            } else {
                laneEnds[lane] = maxOf(laneEnds[lane], segment.endDayIndex)
            }
        } else {
            laneEnds[lane] = segment.endDayIndex
        }
        segment.copy(laneIndex = lane)
    }
}

/** 泳道未占满可用宽度时用 [preferredLaneWidthPx]；超出时均匀压缩以全部可见 */
internal fun computeTimelineLaneWidthPx(
    laneCount: Int,
    availableWidthPx: Float,
    preferredLaneWidthPx: Float,
): Float {
    if (laneCount <= 0) return preferredLaneWidthPx
    val needed = laneCount * preferredLaneWidthPx
    return if (needed <= availableWidthPx) {
        preferredLaneWidthPx
    } else {
        availableWidthPx / laneCount
    }
}
