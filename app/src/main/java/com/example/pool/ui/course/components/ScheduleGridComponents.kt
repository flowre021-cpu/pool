package com.example.pool.ui.course.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import com.example.pool.ui.home.AgendaOverlapGap
import com.example.pool.ui.home.TimedLayoutEntry
import com.example.pool.ui.home.computeOverlapBounds
import com.example.pool.ui.home.layoutOverlappingEntries
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pool.ui.course.model.Course
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.ui.course.model.TimeSlot
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.dayOfWeekShort
import com.example.pool.util.formatMinutesOfDay
import java.time.LocalDate

internal val ScheduleSidebarWidth = 52.dp
internal val ScheduleSectionRowHeight = 64.dp
internal val ScheduleCourseCellInset = 2.dp

data class ScheduleEmptyCellKey(val day: Int, val section: Int)

private data class NormalizedCourse(
    val course: Course,
    val startSection: Int,
    val endSection: Int,
) {
    val layoutKey: String get() = course.id.toString()
}

private fun normalizeCourses(courses: List<Course>, maxSection: Int): List<NormalizedCourse> =
    courses.mapNotNull { course ->
        if (course.dayOfWeek !in 1..7) return@mapNotNull null
        val start = course.startSection.coerceIn(1, maxSection)
        val end = course.endSection.coerceIn(start, maxSection)
        NormalizedCourse(course, start, end)
    }

@Composable
fun ScheduleWeekHeader(
    weekDates: List<LocalDate>,
    monthLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(0.25.dp, SchedulePastelColors.gridLine)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(ScheduleSidebarWidth)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.labelSmall,
                color = PoolColors.Accent,
                textAlign = TextAlign.Center,
            )
        }
        weekDates.forEachIndexed { index, date ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = dayOfWeekShort(index + 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = PoolColors.Accent,
                )
                Text(
                    text = "${date.dayOfMonth}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = PoolColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun ScheduleSectionTimeLabel(
    slot: TimeSlot,
    rowHeight: Dp,
    onSectionClick: (TimeSlot) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .width(ScheduleSidebarWidth)
            .height(rowHeight)
            .background(if (isPressed) PoolColors.Accent.copy(alpha = 0.08f) else Color.White)
            .border(0.25.dp, SchedulePastelColors.gridLine)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSectionClick(slot)
                },
            )
            .padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "${slot.sectionNumber}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = PoolColors.Accent,
        )
        Text(
            text = formatMinutesOfDay(slot.startTimeMinutes),
            style = MaterialTheme.typography.labelSmall,
            color = PoolColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            text = formatMinutesOfDay(slot.endTimeMinutes),
            style = MaterialTheme.typography.labelSmall,
            color = PoolColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun ScheduleCourseCardContent(course: Course, compact: Boolean = false) {
    val primaryText = SchedulePastelColors.textOnCard(course.cardColor)
    val secondaryText = SchedulePastelColors.textOnCard(course.cardColor, secondary = true)
    val nameMaxLines = if (compact) 2 else 4
    val detailMaxLines = if (compact) 1 else 3
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = course.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = primaryText,
            textAlign = TextAlign.Center,
            maxLines = nameMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        if (!course.classroom.isNullOrBlank()) {
            Text(
                text = "@${course.classroom}",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText,
                textAlign = TextAlign.Center,
                maxLines = detailMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!course.teacher.isNullOrBlank()) {
            Text(
                text = course.teacher,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText,
                textAlign = TextAlign.Center,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!compact && !course.note.isNullOrBlank()) {
            Text(
                text = course.note,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ScheduleEmptyCell(
    rowHeight: Dp,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val showFeedback = isPressed || isHighlighted

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(
                if (showFeedback) PoolColors.AccentPrimary.copy(alpha = 0.16f) else Color.White,
            )
            .border(
                width = if (isHighlighted) 1.5.dp else 0.25.dp,
                color = if (isHighlighted) PoolColors.AccentPrimary else SchedulePastelColors.gridLine,
            )
            .clickable(
                enabled = !isHighlighted,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showFeedback) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = PoolColors.AccentPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ScheduleCourseCard(
    entry: NormalizedCourse,
    onCourseClick: (Course) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onCourseClick(entry.course) },
        color = entry.course.cardColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        ScheduleCourseCardContent(entry.course, compact = compact)
    }
}

/** 单日列：背景网格 + 跨节合并卡片 overlay */
@Composable
private fun ScheduleDayColumn(
    day: Int,
    timeSlots: List<TimeSlot>,
    dayCourses: List<NormalizedCourse>,
    rowHeight: Dp,
    highlightedEmptyCell: ScheduleEmptyCellKey?,
    onEmptyCellClick: (dayOfWeek: Int, section: Int) -> Unit,
    onCourseClick: (Course) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnHeight = rowHeight * timeSlots.size
    val layoutEntries = remember(dayCourses) {
        dayCourses.map { entry ->
            TimedLayoutEntry(
                key = entry.layoutKey,
                startMinutes = entry.startSection,
                endMinutes = entry.endSection + 1,
            )
        }
    }
    val positioned = remember(layoutEntries) { layoutOverlappingEntries(layoutEntries) }
    val positionByKey = remember(positioned) { positioned.associateBy { it.key } }

    BoxWithConstraints(
        modifier = modifier
            .height(columnHeight)
            .fillMaxWidth(),
    ) {
        val density = LocalDensity.current
        val gapPx = with(density) { AgendaOverlapGap.toPx() }
        val columnWidthPx = constraints.maxWidth.toFloat()

        Column(modifier = Modifier.fillMaxSize()) {
            timeSlots.forEach { slot ->
                val occupied = dayCourses.any { slot.sectionNumber in it.startSection..it.endSection }
                if (!occupied) {
                    ScheduleEmptyCell(
                        rowHeight = rowHeight,
                        isHighlighted = highlightedEmptyCell?.day == day &&
                            highlightedEmptyCell.section == slot.sectionNumber,
                        onClick = { onEmptyCellClick(day, slot.sectionNumber) },
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .background(Color.White)
                            .border(0.25.dp, SchedulePastelColors.gridLine),
                    )
                }
            }
        }

        dayCourses.forEach { entry ->
            val layout = positionByKey[entry.layoutKey] ?: return@forEach
            val span = entry.endSection - entry.startSection + 1
            val topOffset = rowHeight * (entry.startSection - 1)
            val (xPx, widthPx) = computeOverlapBounds(
                columnWidthPx = columnWidthPx,
                column = layout.column,
                columnCount = layout.columnCount,
                gapPx = gapPx,
            )
            val xDp = with(density) { xPx.toDp() }
            val widthDp = with(density) { widthPx.toDp() }
            Box(
                modifier = Modifier
                    .offset(x = xDp, y = topOffset)
                    .width(widthDp)
                    .height(rowHeight * span)
                    .padding(ScheduleCourseCellInset),
            ) {
                ScheduleCourseCard(
                    entry = entry,
                    onCourseClick = onCourseClick,
                    compact = layout.columnCount > 1,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * 星期表头吸顶；连续节次合并为单卡片 overlay 显示。
 */
@Composable
fun ScheduleScrollableContent(
    timeSlots: List<TimeSlot>,
    courses: List<Course>,
    weekDates: List<LocalDate>,
    highlightedEmptyCell: ScheduleEmptyCellKey?,
    onEmptyCellClick: (dayOfWeek: Int, section: Int) -> Unit,
    onCourseClick: (Course) -> Unit,
    onSectionClick: (TimeSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (timeSlots.isEmpty() || weekDates.isEmpty()) return

    val maxSection = timeSlots.maxOf { it.sectionNumber }
    val normalizedCourses = remember(courses, maxSection) {
        normalizeCourses(courses, maxSection)
    }
    val monthLabel = "${weekDates.first().monthValue}月"
    val scrollState = rememberScrollState()
    val rowHeight = ScheduleSectionRowHeight

    Column(modifier = modifier.fillMaxSize()) {
        ScheduleWeekHeader(
            weekDates = weekDates,
            monthLabel = monthLabel,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column {
                    timeSlots.forEach { slot ->
                        ScheduleSectionTimeLabel(
                            slot = slot,
                            rowHeight = rowHeight,
                            onSectionClick = onSectionClick,
                        )
                    }
                }
                Row(modifier = Modifier.weight(1f)) {
                    (1..7).forEach { day ->
                        ScheduleDayColumn(
                            day = day,
                            timeSlots = timeSlots,
                            dayCourses = normalizedCourses.filter { it.course.dayOfWeek == day },
                            rowHeight = rowHeight,
                            highlightedEmptyCell = highlightedEmptyCell,
                            onEmptyCellClick = onEmptyCellClick,
                            onCourseClick = onCourseClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
