package com.example.pool.ui.affair.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pool.ui.components.PoolRowHorizontalPadding
import com.example.pool.ui.course.components.CourseEditActionText
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.formatAffairBoundary
import com.example.pool.util.formatAffairDate
import com.example.pool.util.formatDeadlineTime
import com.example.pool.util.formatOpportunityRange
import com.example.pool.util.ReminderRecurrence
import com.example.pool.util.ReminderRecurrenceMode
import com.example.pool.util.dayOfWeekShort
import com.example.pool.util.formatReminderSummary
import com.example.pool.util.isAffairEndDateOnly
import com.example.pool.util.isAffairStartDateOnly
import com.example.pool.util.isAffairStartDateOnly

private val FormSectionHorizontalPadding = PoolRowHorizontalPadding
private val FormIconSlotWidth = 40.dp
private val FormCardBackground = Color(0xFFF5F5F5)

private val FormSectionTitleStyle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.Medium,
    color = PoolColors.TextPrimary,
)

private val FormBodyTextStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextPrimary,
)

private val FormPlaceholderStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = Color(0xFF9E9E9E),
)

private val FormHelperTextStyle = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextSecondary,
)

@Composable
private fun AffairFormIconSlot(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(FormIconSlotWidth)
            .padding(end = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PoolColors.AccentPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun AffairBoundaryDateRow(
    label: String,
    dateMillis: Long?,
    isEndBoundary: Boolean,
    onSetDateClick: () -> Unit,
    onClearDateClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AffairFormIconSlot(icon = Icons.Default.CalendarMonth)
        Text(
            text = label,
            style = FormBodyTextStyle,
            modifier = Modifier.width(36.dp),
        )
        Text(
            text = dateMillis?.let { formatAffairBoundary(it, isEndBoundary = isEndBoundary) } ?: "未设置",
            style = if (dateMillis != null) FormBodyTextStyle else FormPlaceholderStyle,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CourseEditActionText(text = "设置日期", onClick = onSetDateClick)
            if (dateMillis != null && onClearDateClick != null) {
                CourseEditActionText(text = "清除", onClick = onClearDateClick)
            }
        }
    }
}

@Composable
private fun AffairBoundaryTimeRow(
    label: String,
    boundaryMillis: Long?,
    isEndBoundary: Boolean,
    onSetTimeClick: () -> Unit,
    onClearTimeClick: (() -> Unit)?,
) {
    val hasExplicitTime = boundaryMillis != null && when {
        isEndBoundary -> !isAffairEndDateOnly(boundaryMillis)
        else -> !isAffairStartDateOnly(boundaryMillis)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AffairFormIconSlot(icon = Icons.Default.AccessTime)
        Text(
            text = label,
            style = FormBodyTextStyle,
            modifier = Modifier.width(36.dp),
        )
        Text(
            text = when {
                boundaryMillis == null -> "未设置"
                hasExplicitTime -> formatDeadlineTime(boundaryMillis)
                else -> "未设置（可选）"
            },
            style = if (hasExplicitTime) FormBodyTextStyle else FormPlaceholderStyle,
            modifier = Modifier.weight(1f),
        )
        CourseEditActionText(text = "设置时间", onClick = onSetTimeClick)
        if (hasExplicitTime && onClearTimeClick != null) {
            CourseEditActionText(text = "清除", onClick = onClearTimeClick)
        }
    }
}

@Composable
fun ReminderScheduleSection(
    startMillis: Long?,
    recurrence: ReminderRecurrence,
    onSetDateClick: () -> Unit,
    onSetTimeClick: () -> Unit,
    onClearDate: () -> Unit,
    onClearTime: () -> Unit,
    onRecurrenceModeChange: (ReminderRecurrenceMode) -> Unit,
    onToggleWeeklyDay: (Int) -> Unit,
) {
    val hasTime = startMillis != null && !isAffairStartDateOnly(startMillis)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormSectionHorizontalPadding, vertical = 16.dp),
    ) {
        Text(text = "提醒", style = FormSectionTitleStyle)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatReminderSummary(startMillis, recurrence),
            style = if (startMillis != null || recurrence.mode != ReminderRecurrenceMode.NONE) {
                FormBodyTextStyle
            } else {
                FormPlaceholderStyle
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = FormCardBackground,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(text = "重复", style = FormSectionTitleStyle.copy(fontSize = 14.sp))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReminderRecurrenceMode.entries.forEach { mode ->
                        ReminderRecurrenceModeChip(
                            label = when (mode) {
                                ReminderRecurrenceMode.NONE -> "不重复"
                                ReminderRecurrenceMode.DAILY -> "每天"
                                ReminderRecurrenceMode.WEEKLY -> "每周"
                            },
                            selected = recurrence.mode == mode,
                            onClick = { onRecurrenceModeChange(mode) },
                        )
                    }
                }
                if (recurrence.mode == ReminderRecurrenceMode.WEEKLY) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        (1..7).forEach { day ->
                            ReminderWeeklyDayChip(
                                label = dayOfWeekShort(day),
                                selected = day in recurrence.weeklyDays,
                                onClick = { onToggleWeeklyDay(day) },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AffairFormIconSlot(icon = Icons.Default.CalendarMonth)
                    Text(
                        text = startMillis?.let { formatAffairDate(it) } ?: "日期 · 未设置",
                        style = if (startMillis != null) FormBodyTextStyle else FormPlaceholderStyle,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CourseEditActionText(text = "设置日期", onClick = onSetDateClick)
                        if (startMillis != null) {
                            CourseEditActionText(text = "清除", onClick = onClearDate)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AffairFormIconSlot(icon = Icons.Default.AccessTime)
                    Text(
                        text = when {
                            startMillis == null -> "时间 · 未设置"
                            isAffairStartDateOnly(startMillis) -> "时间 · 未设置"
                            else -> formatDeadlineTime(startMillis)
                        },
                        style = if (hasTime) FormBodyTextStyle else FormPlaceholderStyle,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CourseEditActionText(text = "设置时间", onClick = onSetTimeClick)
                        if (hasTime) {
                            CourseEditActionText(text = "清除", onClick = onClearTime)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (recurrence.mode) {
                ReminderRecurrenceMode.DAILY ->
                    "每天重复；可配合时间，如「每天 09:00 喝水」"
                ReminderRecurrenceMode.WEEKLY ->
                    "可多选星期，如「每周三 取快递」"
                ReminderRecurrenceMode.NONE ->
                    "日期与时间均可不填；仅标题也可作为随时提示"
            },
            style = FormHelperTextStyle,
        )
    }
}

@Composable
private fun ReminderRecurrenceModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) PoolColors.AccentPrimary else PoolColors.Divider
    val background = if (selected) PoolColors.AccentPrimary.copy(alpha = 0.12f) else Color.White
    Text(
        text = label,
        style = FormBodyTextStyle.copy(
            color = if (selected) PoolColors.AccentPrimary else PoolColors.TextSecondary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        ),
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(background, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun ReminderWeeklyDayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) PoolColors.AccentPrimary else PoolColors.Divider
    val background = if (selected) PoolColors.AccentPrimary.copy(alpha = 0.12f) else Color.White
    Text(
        text = label,
        style = FormBodyTextStyle.copy(
            fontSize = 13.sp,
            color = if (selected) PoolColors.AccentPrimary else PoolColors.TextSecondary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        ),
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(background, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp),
    )
}

@Composable
fun OpportunityPeriodSection(
    startMillis: Long?,
    endMillis: Long?,
    onSetStartDateClick: () -> Unit,
    onSetStartTimeClick: () -> Unit,
    onSetEndDateClick: () -> Unit,
    onSetEndTimeClick: () -> Unit,
    onClearStart: () -> Unit,
    onClearEnd: () -> Unit,
    onClearStartTime: () -> Unit,
    onClearEndTime: () -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormSectionHorizontalPadding, vertical = 16.dp),
    ) {
        Text(text = "周期", style = FormSectionTitleStyle)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatOpportunityRange(startMillis, endMillis),
            style = if (startMillis != null || endMillis != null) FormBodyTextStyle else FormPlaceholderStyle,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = FormCardBackground,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(text = "开始", style = FormSectionTitleStyle.copy(fontSize = 14.sp))
                Spacer(modifier = Modifier.height(8.dp))
                AffairBoundaryDateRow(
                    label = "日期",
                    dateMillis = startMillis,
                    isEndBoundary = false,
                    onSetDateClick = onSetStartDateClick,
                    onClearDateClick = if (startMillis != null) onClearStart else null,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AffairBoundaryTimeRow(
                    label = "时间",
                    boundaryMillis = startMillis,
                    isEndBoundary = false,
                    onSetTimeClick = onSetStartTimeClick,
                    onClearTimeClick = if (
                        startMillis != null && !isAffairStartDateOnly(startMillis)
                    ) {
                        onClearStartTime
                    } else {
                        null
                    },
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "截止", style = FormSectionTitleStyle.copy(fontSize = 14.sp))
                Spacer(modifier = Modifier.height(8.dp))
                AffairBoundaryDateRow(
                    label = "日期",
                    dateMillis = endMillis,
                    isEndBoundary = true,
                    onSetDateClick = onSetEndDateClick,
                    onClearDateClick = if (endMillis != null) onClearEnd else null,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AffairBoundaryTimeRow(
                    label = "时间",
                    boundaryMillis = endMillis,
                    isEndBoundary = true,
                    onSetTimeClick = onSetEndTimeClick,
                    onClearTimeClick = if (
                        endMillis != null && !isAffairEndDateOnly(endMillis)
                    ) {
                        onClearEndTime
                    } else {
                        null
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (startMillis != null || endMillis != null) {
                CourseEditActionText(text = "清除全部", onClick = onClearAll)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "可设日期区间或单个时间点；时间为可选项",
            style = FormHelperTextStyle,
        )
    }
}

@Composable
fun EventTimePeriodSection(
    startMillis: Long?,
    endMillis: Long?,
    onSetDateClick: () -> Unit,
    onSetStartTimeClick: () -> Unit,
    onSetEndTimeClick: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Text(
            text = "时间段",
            style = FormSectionTitleStyle,
            modifier = Modifier.padding(horizontal = FormSectionHorizontalPadding),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FormSectionHorizontalPadding),
            shape = RoundedCornerShape(10.dp),
            color = FormCardBackground,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(text = "时间段 1", style = FormSectionTitleStyle)

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AffairFormIconSlot(icon = Icons.Default.CalendarMonth)
                    Text(
                        text = startMillis?.let { formatAffairDate(it) } ?: "未设置",
                        style = if (startMillis != null) FormBodyTextStyle else FormPlaceholderStyle,
                        modifier = Modifier.weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CourseEditActionText(text = "设置日期", onClick = onSetDateClick)
                        if (startMillis != null || endMillis != null) {
                            CourseEditActionText(text = "清除", onClick = onClear)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AffairFormIconSlot(icon = Icons.Default.AccessTime)
                    Text(
                        text = startMillis?.let { "开始 ${formatDeadlineTime(it)}" } ?: "开始 · 未设置",
                        style = if (startMillis != null) FormBodyTextStyle else FormPlaceholderStyle,
                        modifier = Modifier.weight(1f),
                    )
                    CourseEditActionText(text = "设置时间", onClick = onSetStartTimeClick)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AffairFormIconSlot(icon = Icons.Default.AccessTime)
                    Text(
                        text = endMillis?.let { "结束 ${formatDeadlineTime(it)}" } ?: "结束 · 未设置",
                        style = if (endMillis != null) FormBodyTextStyle else FormPlaceholderStyle,
                        modifier = Modifier.weight(1f),
                    )
                    CourseEditActionText(text = "设置时间", onClick = onSetEndTimeClick)
                }
            }
        }
    }
}
