package com.example.pool.ui.course.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import com.example.pool.ui.components.PoolDivider
import com.example.pool.ui.course.CardColorPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import com.example.pool.ui.course.model.CourseColorPalettes
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.ui.course.model.TimeSlot
import com.example.pool.ui.course.model.colorFromHsv
import com.example.pool.ui.course.model.colorToDisplayHex
import com.example.pool.ui.course.model.colorToHsv
import com.example.pool.ui.course.model.parseHexColor
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.DEFAULT_WEEK_COUNT
import com.example.pool.util.formatMinutesOfDay
import com.example.pool.util.formatSelectedWeeksSummary
import com.example.pool.util.toggleWeekSelection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseColorPickerDialog(
    selected: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val context = LocalContext.current
    val colorPrefs = remember { CardColorPreferences(context) }
    var showFineTune by remember { mutableStateOf(false) }
    var pickerUsesCustom by remember { mutableStateOf(colorPrefs.usesCustomPalette()) }
    var customPalette by remember { mutableStateOf(colorPrefs.getCustomPaletteColors()) }

    val paletteColors = remember(pickerUsesCustom, customPalette) {
        if (pickerUsesCustom && customPalette.isNotEmpty()) {
            customPalette
        } else {
            CourseColorPalettes.defaultColors
        }
    }

    fun togglePaletteMode() {
        if (!pickerUsesCustom && customPalette.isEmpty()) {
            Toast.makeText(context, "自定义色卡为空，请先在微调中保存颜色", Toast.LENGTH_SHORT).show()
            return
        }
        pickerUsesCustom = !pickerUsesCustom
        colorPrefs.setUsesCustomPalette(pickerUsesCustom)
    }

    val initialHsv = remember(selected) { colorToHsv(selected) }
    var previewColor by remember(selected) { mutableStateOf(selected) }
    var hue by remember(selected) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(selected) { mutableFloatStateOf(initialHsv[1].coerceIn(0.35f, 1f)) }
    var brightness by remember(selected) { mutableFloatStateOf(initialHsv[2].coerceIn(0.5f, 1f)) }
    var hexInput by remember(selected) { mutableStateOf(colorToDisplayHex(selected)) }
    var hexError by remember { mutableStateOf(false) }
    var syncingHexFromPreview by remember { mutableStateOf(false) }

    fun applyPreviewColor(color: Color) {
        previewColor = color
        val hsv = colorToHsv(color)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        syncingHexFromPreview = true
        hexInput = colorToDisplayHex(color)
        hexError = false
    }

    fun updateFromHsv() {
        applyPreviewColor(colorFromHsv(hue, saturation, brightness))
    }

    LaunchedEffect(hexInput, syncingHexFromPreview) {
        if (syncingHexFromPreview) {
            syncingHexFromPreview = false
            return@LaunchedEffect
        }
        val parsed = parseHexColor(hexInput)
        if (parsed != null) {
            hexError = false
            previewColor = parsed
            val hsv = colorToHsv(parsed)
            hue = hsv[0]
            saturation = hsv[1]
            brightness = hsv[2]
        } else if (hexInput.isNotBlank()) {
            hexError = true
        } else {
            hexError = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PoolColors.Background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = "选择卡片颜色",
                    color = PoolColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(previewColor)
                            .border(1.dp, PoolColors.Divider, RoundedCornerShape(10.dp)),
                    )
                    Text("预览", color = PoolColors.TextSecondary)
                }

                if (paletteColors.isNotEmpty() || pickerUsesCustom) {
                    val paletteHint = when {
                        pickerUsesCustom && customPalette.isEmpty() -> "自定义色卡为空"
                        pickerUsesCustom -> "长按色块可删除"
                        else -> "点击右侧切换自定义色卡"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "色卡",
                            color = PoolColors.AccentPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            modifier = Modifier.clickable(onClick = { togglePaletteMode() }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (pickerUsesCustom) "自定义" else "默认",
                                color = PoolColors.AccentPrimary,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "切换色卡",
                                tint = PoolColors.AccentPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Text(
                        text = paletteHint,
                        color = PoolColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                    if (paletteColors.isNotEmpty()) {
                        ColorSwatchRow(
                            colors = paletteColors,
                            selected = previewColor,
                            onSelect = ::applyPreviewColor,
                            onLongPress = if (pickerUsesCustom) {
                                { color ->
                                    customPalette = colorPrefs.removeCustomColor(color)
                                    if (customPalette.isEmpty()) {
                                        pickerUsesCustom = false
                                        colorPrefs.setUsesCustomPalette(false)
                                        Toast.makeText(context, "已删除，已切回默认色卡", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "已从自定义色卡删除", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                null
                            },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    } else {
                        Text(
                            text = "暂无颜色",
                            color = PoolColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    PoolDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFineTune = !showFineTune }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (showFineTune) "收起微调" else "微调颜色",
                        color = PoolColors.AccentPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (showFineTune) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        HsvGradientSlider(
                            label = "色相",
                            value = hue,
                            onValueChange = {
                                hue = it
                                updateFromHsv()
                            },
                            valueRange = 0f..360f,
                            thumbColor = previewColor,
                            gradientColors = listOf(
                                colorFromHsv(0f, 1f, 1f),
                                colorFromHsv(60f, 1f, 1f),
                                colorFromHsv(120f, 1f, 1f),
                                colorFromHsv(180f, 1f, 1f),
                                colorFromHsv(240f, 1f, 1f),
                                colorFromHsv(300f, 1f, 1f),
                                colorFromHsv(360f, 1f, 1f),
                            ),
                        )
                        HsvGradientSlider(
                            label = "饱和度",
                            value = saturation,
                            onValueChange = {
                                saturation = it
                                updateFromHsv()
                            },
                            valueRange = 0.2f..1f,
                            thumbColor = previewColor,
                            gradientColors = (0..8).map { step ->
                                val s = 0.2f + step * 0.1f
                                colorFromHsv(hue, s, brightness)
                            },
                        )
                        HsvGradientSlider(
                            label = "明度",
                            value = brightness,
                            onValueChange = {
                                brightness = it
                                updateFromHsv()
                            },
                            valueRange = 0.4f..1f,
                            thumbColor = previewColor,
                            gradientColors = (0..8).map { step ->
                                val v = 0.4f + step * 0.075f
                                colorFromHsv(hue, saturation, v)
                            },
                        )
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { raw ->
                                syncingHexFromPreview = false
                                hexInput = raw.trim()
                            },
                            label = { Text("HEX") },
                            placeholder = { Text("#RRGGBB") },
                            singleLine = true,
                            isError = hexError,
                            supportingText = {
                                if (hexError) Text("无效 HEX", color = PoolColors.DeleteRed)
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PoolColors.TextPrimary,
                                unfocusedTextColor = PoolColors.TextPrimary,
                                focusedBorderColor = PoolColors.AccentPrimary,
                                unfocusedBorderColor = PoolColors.Divider,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                        TextButton(
                            onClick = {
                                customPalette = colorPrefs.addCustomColor(previewColor)
                                pickerUsesCustom = true
                                Toast.makeText(context, "已保存到自定义色卡", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Text("保存到自定义色卡", color = PoolColors.AccentPrimary)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = PoolColors.TextSecondary)
                    }
                    TextButton(
                        onClick = {
                            onColorSelected(previewColor)
                            onDismiss()
                        },
                    ) {
                        Text("确定", color = PoolColors.AccentPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun HsvGradientSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    thumbColor: Color,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 12.dp)) {
        Text(label, color = PoolColors.TextSecondary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(gradientColors)),
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ColorSwatchRow(
    colors: List<Color>,
    selected: Color,
    onSelect: (Color) -> Unit,
    onLongPress: ((Color) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        colors.forEach { color ->
            val isSelected = color.toArgb() == selected.toArgb()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, CircleShape)
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) PoolColors.AccentPrimary else Color.Transparent,
                        shape = CircleShape,
                    )
                    .combinedClickable(
                        onClick = { onSelect(color) },
                        onLongClick = { onLongPress?.invoke(color) },
                    ),
            )
        }
    }
}

/** @deprecated 使用 [CourseColorPickerDialog] */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PastelColorPickerDialog(
    selected: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) = CourseColorPickerDialog(selected, onDismiss, onColorSelected)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekSelectionDialog(
    selectedWeeks: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
    totalWeeks: Int = DEFAULT_WEEK_COUNT,
) {
    var draftWeeks by remember(selectedWeeks, totalWeeks) {
        mutableStateOf(selectedWeeks.filter { it in 1..totalWeeks })
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PoolColors.Block1,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("自定义上课周次", color = PoolColors.TextPrimary)
                WeekSelectionGrid(
                    selectedWeeks = draftWeeks,
                    onSelectionChange = { draftWeeks = it },
                    totalWeeks = totalWeeks,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = PoolColors.TextPrimary)
                    }
                    TextButton(
                        onClick = {
                            onConfirm(draftWeeks)
                            onDismiss()
                        },
                    ) {
                        Text("确定", color = PoolColors.Accent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeekSelectionGrid(
    selectedWeeks: List<Int>,
    onSelectionChange: (List<Int>) -> Unit,
    totalWeeks: Int = DEFAULT_WEEK_COUNT,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = formatSelectedWeeksSummary(selectedWeeks, totalWeeks),
            color = PoolColors.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "点击取消某周表示该周不上课",
            color = PoolColors.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (1..totalWeeks).forEach { week ->
                val isSelected = week in selectedWeeks
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) PoolColors.Accent.copy(alpha = 0.2f) else PoolColors.Block1,
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) PoolColors.Accent else SchedulePastelColors.gridLine,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable {
                            onSelectionChange(toggleWeekSelection(selectedWeeks, week, totalWeeks))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$week",
                        color = if (isSelected) PoolColors.Accent else PoolColors.TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
fun WeekRangePickerDialog(
    weekStart: Int,
    weekEnd: Int,
    onDismiss: () -> Unit,
    onConfirm: (start: Int, end: Int) -> Unit,
) {
    var start by remember { mutableIntStateOf(weekStart) }
    var end by remember { mutableIntStateOf(weekEnd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PoolColors.Block1,
        titleContentColor = PoolColors.TextPrimary,
        textContentColor = PoolColors.TextPrimary,
        title = { Text("选择周次") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = start.toString(),
                    onValueChange = { start = it.toIntOrNull()?.coerceIn(1, 30) ?: start },
                    label = { Text("起始周") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PoolColors.Accent),
                )
                OutlinedTextField(
                    value = end.toString(),
                    onValueChange = { end = it.toIntOrNull()?.coerceIn(1, 30) ?: end },
                    label = { Text("结束周") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PoolColors.Accent),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(start.coerceAtMost(end), end.coerceAtLeast(start))
                },
            ) {
                Text("确定", color = PoolColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = PoolColors.TextPrimary)
            }
        },
        tonalElevation = AlertDialogDefaults.TonalElevation,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfWeekPickerDialog(
    selected: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = PoolColors.Block1) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "选择星期",
                    color = PoolColors.Accent,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                (1..7).forEach { day ->
                    val label = when (day) {
                        1 -> "周一"
                        2 -> "周二"
                        3 -> "周三"
                        4 -> "周四"
                        5 -> "周五"
                        6 -> "周六"
                        7 -> "周日"
                        else -> ""
                    }
                    Text(
                        text = label,
                        color = if (day == selected) PoolColors.Accent else PoolColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelected(day)
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionRangePickerDialog(
    maxSection: Int,
    startSection: Int,
    endSection: Int,
    onDismiss: () -> Unit,
    onConfirm: (start: Int, end: Int) -> Unit,
) {
    var draftStart by remember(startSection, endSection) {
        mutableIntStateOf(startSection.coerceIn(1, maxSection))
    }
    var draftEnd by remember(startSection, endSection) {
        mutableIntStateOf(endSection.coerceIn(startSection.coerceIn(1, maxSection), maxSection))
    }

    fun onSectionClick(section: Int) {
        when {
            draftStart == draftEnd -> {
                if (section == draftStart) return
                if (section > draftStart) draftEnd = section else {
                    draftEnd = draftStart
                    draftStart = section
                }
            }
            section < draftStart -> draftStart = section
            section > draftEnd -> draftEnd = section
            else -> {
                draftStart = section
                draftEnd = section
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PoolColors.Block1,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("选择节次", color = PoolColors.TextPrimary)
                Text(
                    text = if (draftStart == draftEnd) {
                        "第 $draftStart 节"
                    } else {
                        "第 $draftStart - $draftEnd 节"
                    },
                    color = PoolColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                Text(
                    text = "点击节次选择范围，再次点击可调整起止",
                    color = PoolColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (1..maxSection).forEach { section ->
                        val inRange = section in draftStart..draftEnd
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (inRange) PoolColors.Accent.copy(alpha = 0.2f) else PoolColors.Block1,
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (inRange) PoolColors.Accent else SchedulePastelColors.gridLine,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable { onSectionClick(section) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$section",
                                color = if (inRange) PoolColors.Accent else PoolColors.TextSecondary,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = PoolColors.TextPrimary)
                    }
                    TextButton(
                        onClick = {
                            onConfirm(draftStart, draftEnd)
                            onDismiss()
                        },
                    ) {
                        Text("确定", color = PoolColors.Accent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotSettingsDialog(
    timeSlots: List<TimeSlot>,
    onDismiss: () -> Unit,
    onSaveSlot: (TimeSlot) -> Unit,
) {
    var editingSection by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PoolColors.Block1,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("节次时间设置", color = PoolColors.Accent)
                timeSlots.forEach { slot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingSection = slot.sectionNumber }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("第 ${slot.sectionNumber} 节", color = PoolColors.TextPrimary)
                        Text(
                            "${formatMinutesOfDay(slot.startTimeMinutes)} - " +
                                formatMinutesOfDay(slot.endTimeMinutes),
                            color = PoolColors.TextSecondary,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = PoolColors.TextPrimary)
                    }
                }
            }
        }
    }

    editingSection?.let { section ->
        val slot = timeSlots.firstOrNull { it.sectionNumber == section } ?: return@let
        EditSingleTimeSlotDialog(
            slot = slot,
            onDismiss = { editingSection = null },
            onSave = { updated ->
                onSaveSlot(updated)
                editingSection = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSingleTimeSlotDialog(
    slot: TimeSlot,
    onDismiss: () -> Unit,
    onSave: (TimeSlot) -> Unit,
) {
    var startText by remember {
        mutableStateOf(formatMinutesOfDay(slot.startTimeMinutes))
    }
    var endText by remember {
        mutableStateOf(formatMinutesOfDay(slot.endTimeMinutes))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PoolColors.Block1,
        title = { Text("第 ${slot.sectionNumber} 节") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("开始时间 (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PoolColors.Accent),
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("结束时间 (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PoolColors.Accent),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = parseTimeToMinutes(startText)
                    val end = parseTimeToMinutes(endText)
                    if (start != null && end != null && end > start) {
                        onSave(
                            slot.copy(
                                startTimeMinutes = start,
                                endTimeMinutes = end,
                            ),
                        )
                    }
                },
            ) {
                Text("保存", color = PoolColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = PoolColors.TextPrimary)
            }
        },
    )
}

private fun parseTimeToMinutes(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}
