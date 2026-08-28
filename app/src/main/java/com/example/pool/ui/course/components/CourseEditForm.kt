package com.example.pool.ui.course.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pool.ui.components.PoolRowHorizontalPadding
import com.example.pool.ui.course.model.CourseTimePeriod
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.dayOfWeekLabel
import com.example.pool.util.formatSectionRange
import com.example.pool.util.formatSelectedWeeksSummary

private val FormSectionHorizontalPadding = PoolRowHorizontalPadding
private val FormIconSlotWidth = 40.dp
private val FormCardBackground = Color(0xFFF5F5F5)

private val FormSectionTitleStyle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.Medium,
    color = PoolColors.TextPrimary,
)

private val FormInputTextStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextPrimary,
)

private val FormPlaceholderStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = Color(0xFF9E9E9E),
)

private val FormBodyTextStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextPrimary,
)

private val FormHelperTextStyle = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Normal,
    color = PoolColors.TextSecondary,
)

private val FormActionTextStyle = TextStyle(
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,
    color = PoolColors.Accent,
)

/** 仅用于隔离大业务区块 */
@Composable
fun CourseEditSectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = Color.LightGray.copy(alpha = 0.3f),
    )
}

@Composable
private fun CourseEditIconSlot(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(FormIconSlotWidth)
            .padding(end = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PoolColors.AccentPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CourseEditSectionTitle(text: String) {
    Text(text = text, style = FormSectionTitleStyle)
}

@Composable
fun CourseEditActionText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    color: Color = PoolColors.Accent,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = text, style = FormActionTextStyle.copy(color = color))
    }
}

@Composable
fun CourseEditTextFieldSection(
    title: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormSectionHorizontalPadding, vertical = 16.dp),
    ) {
        CourseEditSectionTitle(text = title)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            CourseEditIconSlot(icon = icon)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(if (singleLine) Modifier else Modifier.height(96.dp)),
                textStyle = FormInputTextStyle,
                singleLine = singleLine,
                maxLines = maxLines,
                cursorBrush = SolidColor(PoolColors.Accent),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(text = placeholder, style = FormPlaceholderStyle)
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
fun CourseEditNoteSection(
    value: String,
    onValueChange: (String) -> Unit,
    helperText: String,
    title: String = "课程备注",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormSectionHorizontalPadding, vertical = 16.dp),
    ) {
        CourseEditSectionTitle(text = title)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = helperText, style = FormHelperTextStyle)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            CourseEditIconSlot(icon = Icons.Default.Notes)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .height(96.dp),
                textStyle = FormInputTextStyle,
                singleLine = false,
                maxLines = 4,
                cursorBrush = SolidColor(PoolColors.Accent),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.TopStart) {
                        if (value.isEmpty()) {
                            Text(text = "（可不填）", style = FormPlaceholderStyle)
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
fun CourseEditColorSection(
    cardColor: Color,
    onPickColor: () -> Unit,
    sectionTitle: String = "卡片颜色",
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormSectionHorizontalPadding, vertical = 16.dp),
    ) {
        CourseEditSectionTitle(text = sectionTitle)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPickColor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CourseEditIconSlot(icon = Icons.Default.Palette)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(cardColor, RoundedCornerShape(6.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "点此选择",
                style = FormBodyTextStyle,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun CourseEditTimePeriodsSection(
    timePeriods: List<CourseTimePeriod>,
    maxSection: Int,
    totalWeeks: Int,
    onAddPeriod: () -> Unit,
    onUpdatePeriod: (index: Int, CourseTimePeriod) -> Unit,
    onRemovePeriod: (index: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FormSectionHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CourseEditSectionTitle(text = "时间段")
            Spacer(modifier = Modifier.weight(1f))
            CourseEditActionText(
                text = "添加时间段",
                leadingIcon = Icons.Default.Add,
                onClick = onAddPeriod,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier.padding(horizontal = FormSectionHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            timePeriods.forEachIndexed { index, period ->
                TimePeriodCard(
                    period = period,
                    index = index,
                    maxSection = maxSection,
                    totalWeeks = totalWeeks,
                    canRemove = timePeriods.size > 1,
                    onUpdate = { onUpdatePeriod(index, it) },
                    onRemove = { onRemovePeriod(index) },
                )
            }
        }
    }
}

@Composable
private fun TimePeriodCard(
    period: CourseTimePeriod,
    index: Int,
    maxSection: Int,
    totalWeeks: Int,
    canRemove: Boolean,
    onUpdate: (CourseTimePeriod) -> Unit,
    onRemove: () -> Unit,
) {
    var showSectionPicker by remember { mutableStateOf(false) }
    var showWeekPicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }

    if (showDayPicker) {
        DayOfWeekPickerDialog(
            selected = period.dayOfWeek,
            onDismiss = { showDayPicker = false },
            onSelected = { day ->
                onUpdate(period.copy(dayOfWeek = day))
                showDayPicker = false
            },
        )
    }
    if (showWeekPicker) {
        WeekSelectionDialog(
            selectedWeeks = period.selectedWeeks,
            totalWeeks = totalWeeks,
            onDismiss = { showWeekPicker = false },
            onConfirm = { weeks -> onUpdate(period.copy(selectedWeeks = weeks)) },
        )
    }
    if (showSectionPicker) {
        SectionRangePickerDialog(
            maxSection = maxSection,
            startSection = period.startSection,
            endSection = period.endSection,
            onDismiss = { showSectionPicker = false },
            onConfirm = { start, end ->
                onUpdate(period.copy(startSection = start, endSection = end))
                showSectionPicker = false
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = FormCardBackground,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "时间段 ${index + 1}",
                    style = FormSectionTitleStyle,
                    modifier = Modifier.weight(1f),
                )
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "移除",
                            tint = PoolColors.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CourseEditIconSlot(icon = Icons.Default.CalendarMonth)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = formatSelectedWeeksSummary(period.selectedWeeks, totalWeeks), style = FormBodyTextStyle)
                }
                CourseEditActionText(
                    text = "自定义周次",
                    onClick = { showWeekPicker = true },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CourseEditIconSlot(icon = Icons.Default.CalendarMonth)
                Text(
                    text = dayOfWeekLabel(period.dayOfWeek),
                    style = FormBodyTextStyle,
                    modifier = Modifier.weight(1f),
                )
                CourseEditActionText(
                    text = "修改星期",
                    onClick = { showDayPicker = true },
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CourseEditIconSlot(icon = Icons.Default.AccessTime)
                Text(
                    text = formatSectionRange(period.startSection, period.endSection),
                    style = FormBodyTextStyle,
                    modifier = Modifier.weight(1f),
                )
                CourseEditActionText(
                    text = "修改节次",
                    onClick = { showSectionPicker = true },
                )
            }
        }
    }
}
