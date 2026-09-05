package com.example.pool.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.dayOfWeekShort
import java.time.LocalDate

private val TimelineDayHeight = 72.dp
private val TimelineDateColumnWidth = 56.dp
private val TimelineAxisColumnWidth = 20.dp
private val TimelineLaneWidth = 50.dp
private val TimelineTaskTickLength = 22.dp
private val TimelineAxisStroke = 1.5.dp
private val TimelineTaskStroke = 2.5.dp
private val TimelineOpportunityStroke = 1.5.dp
private val TimelineEndpointRadius = 2.5.dp
private val TimelineReminderRadius = 3.dp
private val TimelineMarkerStaggerStep = 6.dp
private val TimelineReminderHorizontalStep = 5.dp
private val TimelineLabelGapFromLine = 2.dp
private const val TimelineLinePositionFraction = 0.38f
private const val TimelineReminderPositionFraction = 0.72f

@Composable
fun FourteenDayTimelinePage(
    uiState: TimelineUiState,
    onDayClick: (LocalDate) -> Unit,
    onEditTask: (Long) -> Unit,
    onEditOpportunity: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TimelineToolbar()

        if (uiState.days.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "暂无时间轴数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PoolColors.TextSecondary,
                )
            }
            return@Column
        }

        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(PoolColors.Block)
                .verticalScroll(scrollState),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                uiState.days.forEach { day ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TimelineDayHeight)
                            .background(dayBackgroundColor(day.isToday)),
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                uiState.days.forEach { day ->
                    TimelineDayRow(
                        day = day,
                        onClick = { onDayClick(day.date) },
                    )
                }
            }

            TimelineMarkersLayer(
                days = uiState.days,
                opportunitySegments = uiState.opportunitySegments,
                taskMarkers = uiState.taskMarkers,
                reminderMarkers = uiState.reminderMarkers,
                onEditTask = onEditTask,
                onEditOpportunity = onEditOpportunity,
                onEditReminder = onEditReminder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TimelineDayHeight * uiState.days.size),
            )
        }
    }
}

/** 除本日段外为纯白；本日段保留 Block 底色 */
private fun dayBackgroundColor(isToday: Boolean): Color =
    if (isToday) PoolColors.Block else Color.White

@Composable
private fun TimelineToolbar(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "未来 ${TIMELINE_DAY_COUNT} 天",
            style = MaterialTheme.typography.labelLarge,
            color = PoolColors.TextSecondary,
        )
    }
}

@Composable
private fun TimelineDayRow(
    day: TimelineDayState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TimelineDayHeight),
    ) {
        Column(
            modifier = Modifier
                .width(TimelineDateColumnWidth)
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(start = 12.dp, top = 8.dp),
        ) {
            Text(
                text = "${day.date.monthValue}/${day.date.dayOfMonth}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                color = if (day.isToday) PoolColors.AccentPrimary else PoolColors.TextPrimary,
            )
            Text(
                text = dayOfWeekShort(day.date.dayOfWeek.value),
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isToday) PoolColors.AccentPrimary else PoolColors.TextSecondary,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clickable(onClick = onClick),
        )
    }
}

@Composable
private fun TimelineMarkersLayer(
    days: List<TimelineDayState>,
    opportunitySegments: List<TimelineOpportunitySegment>,
    taskMarkers: List<TimelineTaskMarker>,
    reminderMarkers: List<TimelineReminderMarker>,
    onEditTask: (Long) -> Unit,
    onEditOpportunity: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val dayHeightPx = with(density) { TimelineDayHeight.toPx() }
        val dateColPx = with(density) { TimelineDateColumnWidth.toPx() }
        val axisColPx = with(density) { TimelineAxisColumnWidth.toPx() }
        val preferredLaneWidthPx = with(density) { TimelineLaneWidth.toPx() }
        val laneCount = opportunitySegments.maxOfOrNull { it.laneIndex + 1 } ?: 0
        val availableLaneAreaPx = with(density) {
            (maxWidth - TimelineDateColumnWidth - TimelineAxisColumnWidth - 4.dp)
                .toPx()
                .coerceAtLeast(0f)
        }
        val laneWidthPx = computeTimelineLaneWidthPx(
            laneCount = laneCount,
            availableWidthPx = availableLaneAreaPx,
            preferredLaneWidthPx = preferredLaneWidthPx,
        )
        val laneWidthDp = with(density) { laneWidthPx.toDp() }
        val axisX = dateColPx + axisColPx / 2f
        val contentStartPx = dateColPx + axisColPx
        val taskTickPx = with(density) { TimelineTaskTickLength.toPx() }
        val axisStrokePx = with(density) { TimelineAxisStroke.toPx() }
        val taskStrokePx = with(density) { TimelineTaskStroke.toPx() }
        val oppStrokePx = with(density) { TimelineOpportunityStroke.toPx() }
        val endpointRadiusPx = with(density) { TimelineEndpointRadius.toPx() }
        val reminderRadiusPx = with(density) { TimelineReminderRadius.toPx() }
        val staggerStepPx = with(density) { TimelineMarkerStaggerStep.toPx() }
        val reminderHorizontalStepPx = with(density) { TimelineReminderHorizontalStep.toPx() }

        Box(modifier = Modifier.matchParentSize()) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalHeight = dayHeightPx * days.size

            drawLine(
                color = PoolColors.Divider,
                start = Offset(axisX, 0f),
                end = Offset(axisX, totalHeight),
                strokeWidth = axisStrokePx,
                cap = StrokeCap.Round,
            )

            days.forEach { day ->
                val y = dayCenterY(day.dayIndex, dayHeightPx)
                val dotRadius = if (day.isToday) axisStrokePx * 1.6f else axisStrokePx * 0.85f
                drawCircle(
                    color = if (day.isToday) PoolColors.AccentPrimary else PoolColors.Divider,
                    radius = dotRadius,
                    center = Offset(axisX, y),
                )
            }

            opportunitySegments.forEach { segment ->
                val laneX = laneLinePx(contentStartPx, laneWidthPx, segment.laneIndex)
                val top = dayTopY(segment.startDayIndex, dayHeightPx) + dayHeightPx * 0.14f
                val bottom = dayBottomY(segment.endDayIndex, dayHeightPx) - dayHeightPx * 0.14f
                val color = segment.color.copy(alpha = 0.92f)
                drawSegmentWithEndpoints(
                    color = color,
                    x = laneX,
                    top = top,
                    bottom = bottom,
                    strokeWidth = oppStrokePx,
                    endpointRadius = endpointRadiusPx,
                )
            }

            taskMarkers.forEach { marker ->
                val stepPx = effectiveStaggerStepPx(marker.groupSize, staggerStepPx, dayHeightPx)
                val y = staggeredMarkerY(
                    dayIndex = marker.dayIndex,
                    timeFraction = marker.timeFraction,
                    offsetIndex = marker.offsetIndex,
                    groupSize = marker.groupSize,
                    dayHeightPx = dayHeightPx,
                    staggerStepPx = stepPx,
                )
                drawLine(
                    color = marker.color,
                    start = Offset(axisX - taskTickPx / 2f, y),
                    end = Offset(axisX + taskTickPx / 2f, y),
                    strokeWidth = taskStrokePx,
                    cap = StrokeCap.Round,
                )
            }

            reminderMarkers.forEach { marker ->
                val stepPx = effectiveStaggerStepPx(marker.groupSize, staggerStepPx, dayHeightPx)
                val y = staggeredMarkerY(
                    dayIndex = marker.dayIndex,
                    timeFraction = marker.timeFraction,
                    offsetIndex = marker.offsetIndex,
                    groupSize = marker.groupSize,
                    dayHeightPx = dayHeightPx,
                    staggerStepPx = stepPx,
                )
                val reminderX = reminderMarkerX(
                    contentStartPx = contentStartPx,
                    laneWidthPx = laneWidthPx,
                    offsetIndex = marker.offsetIndex,
                    groupSize = marker.groupSize,
                    horizontalStepPx = reminderHorizontalStepPx,
                )
                drawCircle(
                    color = PoolColors.Accent,
                    radius = reminderRadiusPx,
                    center = Offset(reminderX, y),
                )
                drawCircle(
                    color = Color.White,
                    radius = reminderRadiusPx * 0.4f,
                    center = Offset(reminderX, y),
                )
            }
        }

        opportunitySegments.forEach { segment ->
            val lineX = laneLineDp(segment.laneIndex, laneWidthDp)
            val labelX = TimelineDateColumnWidth + TimelineAxisColumnWidth + lineX + TimelineLabelGapFromLine
            val top = TimelineDayHeight * segment.startDayIndex + 4.dp
            val segmentHeight = TimelineDayHeight * (segment.endDayIndex - segment.startDayIndex + 1) - 8.dp
            VerticalTimelineLabel(
                title = segment.title,
                color = segment.color.copy(alpha = 0.98f),
                maxHeight = segmentHeight,
                modifier = Modifier
                    .offset(x = labelX, y = top)
                    .clickable { onEditOpportunity(segment.id) },
            )
        }

        val reminderHitBaseX = TimelineDateColumnWidth + TimelineAxisColumnWidth +
            laneLineDp(TIMELINE_REMINDER_LANE_INDEX, laneWidthDp) +
            laneWidthDp * (TimelineReminderPositionFraction - TimelineLinePositionFraction)

        taskMarkers.forEach { marker ->
            val stepPx = effectiveStaggerStepPx(marker.groupSize, staggerStepPx, dayHeightPx)
            val y = staggeredMarkerY(
                dayIndex = marker.dayIndex,
                timeFraction = marker.timeFraction,
                offsetIndex = marker.offsetIndex,
                groupSize = marker.groupSize,
                dayHeightPx = dayHeightPx,
                staggerStepPx = stepPx,
            )
            val top = with(density) { y.toDp() - 14.dp }
            Box(
                modifier = Modifier
                    .offset(
                        x = TimelineDateColumnWidth + TimelineAxisColumnWidth / 2 - TimelineTaskTickLength / 2,
                        y = top,
                    )
                    .size(width = TimelineTaskTickLength + TimelineAxisColumnWidth, height = 28.dp)
                    .clickable { onEditTask(marker.id) },
            )
        }

        reminderMarkers.forEach { marker ->
            val stepPx = effectiveStaggerStepPx(marker.groupSize, staggerStepPx, dayHeightPx)
            val y = staggeredMarkerY(
                dayIndex = marker.dayIndex,
                timeFraction = marker.timeFraction,
                offsetIndex = marker.offsetIndex,
                groupSize = marker.groupSize,
                dayHeightPx = dayHeightPx,
                staggerStepPx = stepPx,
            )
            val top = with(density) { y.toDp() - 14.dp }
            val horizontalOffset = with(density) {
                reminderMarkerHorizontalOffset(
                    offsetIndex = marker.offsetIndex,
                    groupSize = marker.groupSize,
                    horizontalStepPx = reminderHorizontalStepPx,
                ).toDp()
            }
            Box(
                modifier = Modifier
                    .offset(x = reminderHitBaseX - 10.dp + horizontalOffset, y = top)
                    .size(28.dp)
                    .clickable { onEditReminder(marker.id) },
            )
        }
        }
    }
}

private fun laneLineDp(laneIndex: Int, laneWidth: Dp = TimelineLaneWidth): Dp =
    laneWidth * laneIndex + laneWidth * TimelineLinePositionFraction

@Composable
private fun VerticalTimelineLabel(
    title: String,
    color: Color,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val charStep = 11.dp
    val maxChars = (maxHeight / charStep).toInt().coerceIn(2, 14)
    val display = when {
        title.length <= maxChars -> title
        maxChars <= 1 -> title.take(1)
        else -> title.take(maxChars - 1) + "…"
    }
    Column(
        modifier = modifier
            .width(13.dp)
            .background(Color.White.copy(alpha = 0.88f), RoundedCornerShape(2.dp))
            .padding(horizontal = 1.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        display.forEach { char ->
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                ),
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun laneLinePx(contentStartPx: Float, laneWidthPx: Float, laneIndex: Int): Float =
    contentStartPx + laneWidthPx * laneIndex + laneWidthPx * TimelineLinePositionFraction

private fun DrawScope.drawSegmentWithEndpoints(
    color: Color,
    x: Float,
    top: Float,
    bottom: Float,
    strokeWidth: Float,
    endpointRadius: Float,
    horizontal: Boolean = false,
    endX: Float = x,
) {
    if (horizontal) {
        drawLine(
            color = color,
            start = Offset(x, top),
            end = Offset(endX, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = endpointRadius, center = Offset(x, top))
        drawCircle(color = color, radius = endpointRadius, center = Offset(endX, top))
    } else {
        drawLine(
            color = color,
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = endpointRadius, center = Offset(x, top))
        drawCircle(color = color, radius = endpointRadius, center = Offset(x, bottom))
    }
}

private fun dayCenterY(dayIndex: Int, dayHeightPx: Float): Float =
    dayIndex * dayHeightPx + dayHeightPx / 2f

private fun dayTopY(dayIndex: Int, dayHeightPx: Float): Float =
    dayIndex * dayHeightPx

private fun dayBottomY(dayIndex: Int, dayHeightPx: Float): Float =
    (dayIndex + 1) * dayHeightPx

private fun dayTimeY(dayIndex: Int, timeFraction: Float, dayHeightPx: Float): Float =
    dayIndex * dayHeightPx + dayHeightPx * timeFraction.coerceIn(0f, 1f)

internal fun effectiveStaggerStepPx(
    groupSize: Int,
    staggerStepPx: Float,
    dayHeightPx: Float,
): Float {
    if (groupSize <= 1) return staggerStepPx
    val usable = dayHeightPx - 8f
    val needed = (groupSize - 1) * staggerStepPx
    return if (needed <= usable) staggerStepPx else usable / (groupSize - 1)
}

internal fun reminderMarkerX(
    contentStartPx: Float,
    laneWidthPx: Float,
    offsetIndex: Int,
    groupSize: Int,
    horizontalStepPx: Float,
): Float {
    val baseX = laneLinePx(contentStartPx, laneWidthPx, TIMELINE_REMINDER_LANE_INDEX) +
        laneWidthPx * (TimelineReminderPositionFraction - TimelineLinePositionFraction)
    return baseX + reminderMarkerHorizontalOffset(offsetIndex, groupSize, horizontalStepPx)
}

internal fun reminderMarkerHorizontalOffset(
    offsetIndex: Int,
    groupSize: Int,
    horizontalStepPx: Float,
): Float {
    if (groupSize <= 1) return 0f
    val center = (groupSize - 1) / 2f
    return (offsetIndex - center) * horizontalStepPx
}

internal fun staggeredMarkerY(
    dayIndex: Int,
    timeFraction: Float,
    offsetIndex: Int,
    groupSize: Int,
    dayHeightPx: Float,
    staggerStepPx: Float,
): Float {
    val baseY = dayTimeY(dayIndex, timeFraction, dayHeightPx)
    if (groupSize <= 1) return baseY
    val center = (groupSize - 1) / 2f
    val offset = (offsetIndex - center) * staggerStepPx
    val dayTop = dayTopY(dayIndex, dayHeightPx) + 4f
    val dayBottom = dayBottomY(dayIndex, dayHeightPx) - 4f
    return (baseY + offset).coerceIn(dayTop, dayBottom)
}

fun timelineWindowDates(windowStart: LocalDate = LocalDate.now()): List<LocalDate> =
    (0 until TIMELINE_DAY_COUNT).map { windowStart.plusDays(it.toLong()) }
