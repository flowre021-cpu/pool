package com.example.pool.ui.affair

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDatePickerDialog
import com.example.pool.ui.components.PoolPickerDefaults
import com.example.pool.ui.course.components.CourseColorPickerDialog
import com.example.pool.ui.course.components.CourseEditColorSection
import com.example.pool.ui.course.components.CourseEditSectionDivider
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Place
import com.example.pool.ui.affair.components.EventTimePeriodSection
import com.example.pool.ui.affair.components.OpportunityPeriodSection
import com.example.pool.ui.affair.components.ReminderScheduleSection
import com.example.pool.ui.course.components.CourseEditNoteSection
import com.example.pool.ui.course.components.CourseEditTextFieldSection
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.ui.course.model.toArgbLong
import com.example.pool.ui.course.model.colorFromArgbLong
import com.example.pool.ui.task.components.TaskEditNoteSection
import com.example.pool.ui.task.components.TaskEditTextFieldSection
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.applyDateToBoundaryMillis
import com.example.pool.util.combineAffairDateAndTime
import com.example.pool.util.endOfDayMillis
import com.example.pool.util.datePickerUtcMillisToLocalDate
import com.example.pool.util.isAffairEndDateOnly
import com.example.pool.util.isAffairStartDateOnly
import com.example.pool.util.localDateFromMillis
import com.example.pool.util.localDateToDatePickerUtcMillis
import com.example.pool.util.localTimeFromMillis
import com.example.pool.util.ReminderRecurrence
import com.example.pool.util.ReminderRecurrenceMode
import com.example.pool.util.parseReminderRecurrence
import com.example.pool.util.startOfDayMillis
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private enum class AffairDatePickTarget {
    REMINDER_DAY,
    OPPORTUNITY_START,
    OPPORTUNITY_END,
    EVENT_DAY,
}

private enum class AffairTimePickTarget {
    EVENT_START,
    EVENT_END,
    OPPORTUNITY_START,
    OPPORTUNITY_END,
    REMINDER,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffairEditScreen(
    affairType: AffairType,
    affairId: Long,
    repository: PlannerRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: AffairViewModel = viewModel(
        factory = AffairViewModelFactory(repository, context.applicationContext, affairType),
    )
    val zone = ZoneId.systemDefault()

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var startMillis by remember { mutableLongStateOf(0L) }
    var endMillis by remember { mutableLongStateOf(0L) }
    var hasStart by remember { mutableStateOf(false) }
    var hasEnd by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("") }
    var cardColor by remember { mutableStateOf(SchedulePastelColors.pastelPalette[1]) }

    var showDatePicker by remember { mutableStateOf(false) }
    var datePickTarget by remember { mutableStateOf(AffairDatePickTarget.REMINDER_DAY) }
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickTarget by remember { mutableStateOf(AffairTimePickTarget.EVENT_START) }
    var showColorPicker by remember { mutableStateOf(false) }
    var reminderRecurrence by remember { mutableStateOf(ReminderRecurrence()) }

    LaunchedEffect(affairId) {
        if (affairId != 0L) {
            val affair = viewModel.getAffair(affairId) ?: return@LaunchedEffect
            if (affair.type != affairType) {
                onBack()
                return@LaunchedEffect
            }
            title = affair.title
            note = affair.note.orEmpty()
            location = affair.location.orEmpty()
            if (affair.startAt != null) {
                hasStart = true
                startMillis = affair.startAt
            }
            if (affair.endAt != null) {
                hasEnd = true
                endMillis = affair.endAt
            }
            if (affairType == AffairType.REMINDER) {
                reminderRecurrence = parseReminderRecurrence(affair.recurrenceRule)
            }
            if (affairType == AffairType.EVENT || affairType == AffairType.OPPORTUNITY) {
                cardColor = affair.cardColor?.let { colorFromArgbLong(it) }
                    ?: SchedulePastelColors.pastelPalette[1]
            }
        }
    }

    if (showColorPicker && (affairType == AffairType.EVENT || affairType == AffairType.OPPORTUNITY)) {
        CourseColorPickerDialog(
            selected = cardColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { cardColor = it },
        )
    }

    val canSave = title.isNotBlank() && when (affairType) {
        AffairType.REMINDER -> true
        AffairType.OPPORTUNITY -> hasStart || hasEnd
        AffairType.EVENT -> hasStart && hasEnd
        AffairType.TASK -> false
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text(affairType.editTitle(affairId == 0L)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = PoolColors.AccentPrimary,
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val error = affairSaveValidationError(
                                affairType = affairType,
                                title = title,
                                hasStart = hasStart,
                                hasEnd = hasEnd,
                                reminderRecurrence = reminderRecurrence,
                            )
                            if (error != null) {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            val entity = buildAffairEntity(
                                affairType = affairType,
                                affairId = affairId,
                                title = title.trim(),
                                note = note.trim().ifBlank { null },
                                hasStart = hasStart,
                                startMillis = startMillis,
                                hasEnd = hasEnd,
                                endMillis = endMillis,
                                location = location.trim().ifBlank { null },
                                cardColor = when (affairType) {
                                    AffairType.EVENT, AffairType.OPPORTUNITY -> cardColor.toArgbLong()
                                    else -> null
                                },
                                reminderRecurrence = reminderRecurrence,
                            )
                            viewModel.saveAffair(entity, onBack)
                        },
                    ) {
                        Text(
                            "保存",
                            color = if (canSave) PoolColors.Accent else PoolColors.TextSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PoolColors.NavBar,
                    titleContentColor = PoolColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PoolBlock1 {
                Column {
                    if (affairType == AffairType.EVENT) {
                        CourseEditTextFieldSection(
                            title = "事件标题",
                            icon = Icons.Default.Event,
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "输入标题",
                        )
                        CourseEditSectionDivider()
                        CourseEditTextFieldSection(
                            title = "地点",
                            icon = Icons.Default.Place,
                            value = location,
                            onValueChange = { location = it },
                            placeholder = "（可不填）",
                        )
                        CourseEditSectionDivider()
                        CourseEditColorSection(
                            cardColor = cardColor,
                            onPickColor = { showColorPicker = true },
                        )
                        CourseEditSectionDivider()
                        EventTimePeriodSection(
                            startMillis = startMillis.takeIf { hasStart },
                            endMillis = endMillis.takeIf { hasEnd },
                            onSetDateClick = {
                                datePickTarget = AffairDatePickTarget.EVENT_DAY
                                showDatePicker = true
                            },
                            onSetStartTimeClick = {
                                ensureEventStartDefaults(
                                    zone = zone,
                                    hasStart = hasStart,
                                    startMillis = startMillis,
                                    onUpdateStart = { hasStart = true; startMillis = it },
                                )
                                timePickTarget = AffairTimePickTarget.EVENT_START
                                showTimePicker = true
                            },
                            onSetEndTimeClick = {
                                ensureEventEndDefaults(
                                    zone = zone,
                                    hasStart = hasStart,
                                    hasEnd = hasEnd,
                                    startMillis = startMillis,
                                    onUpdateStart = { hasStart = true; startMillis = it },
                                    onUpdateEnd = { hasEnd = true; endMillis = it },
                                )
                                timePickTarget = AffairTimePickTarget.EVENT_END
                                showTimePicker = true
                            },
                            onClear = {
                                hasStart = false
                                hasEnd = false
                                startMillis = 0L
                                endMillis = 0L
                            },
                        )
                        CourseEditSectionDivider()
                        CourseEditNoteSection(
                            value = note,
                            onValueChange = { note = it },
                            title = "备注",
                            helperText = "备注将显示在日程卡片上",
                        )
                    } else {
                        TaskEditTextFieldSection(
                            title = "标题",
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "输入标题",
                        )
                        CourseEditSectionDivider()
                        TaskEditNoteSection(
                            value = note,
                            onValueChange = { note = it },
                            helperText = "可选备注",
                        )
                        CourseEditSectionDivider()
                        when (affairType) {
                            AffairType.REMINDER -> ReminderScheduleSection(
                                startMillis = startMillis.takeIf { hasStart },
                                recurrence = reminderRecurrence,
                                onSetDateClick = {
                                    datePickTarget = AffairDatePickTarget.REMINDER_DAY
                                    showDatePicker = true
                                },
                                onSetTimeClick = {
                                    ensureReminderTimeDefaults(
                                        zone = zone,
                                        hasStart = hasStart,
                                        startMillis = startMillis,
                                        onUpdate = { hasStart = true; startMillis = it },
                                    )
                                    timePickTarget = AffairTimePickTarget.REMINDER
                                    showTimePicker = true
                                },
                                onClearDate = {
                                    if (hasStart && !isAffairStartDateOnly(startMillis, zone)) {
                                        val time = localTimeFromMillis(startMillis, zone)
                                        startMillis = combineAffairDateAndTime(LocalDate.now(zone), time, zone)
                                    } else {
                                        hasStart = false
                                        startMillis = 0L
                                    }
                                },
                                onClearTime = {
                                    if (!hasStart) return@ReminderScheduleSection
                                    if (isAffairStartDateOnly(startMillis, zone)) {
                                        hasStart = false
                                        startMillis = 0L
                                    } else {
                                        val date = localDateFromMillis(startMillis, zone)
                                        startMillis = startOfDayMillis(date, zone)
                                    }
                                },
                                onRecurrenceModeChange = { mode ->
                                    reminderRecurrence = when (mode) {
                                        ReminderRecurrenceMode.NONE ->
                                            ReminderRecurrence(mode = ReminderRecurrenceMode.NONE)
                                        ReminderRecurrenceMode.DAILY ->
                                            ReminderRecurrence(mode = ReminderRecurrenceMode.DAILY)
                                        ReminderRecurrenceMode.WEEKLY -> {
                                            val days = reminderRecurrence.weeklyDays
                                            ReminderRecurrence(
                                                mode = ReminderRecurrenceMode.WEEKLY,
                                                weeklyDays = days.ifEmpty {
                                                    setOf(LocalDate.now(zone).dayOfWeek.value)
                                                },
                                            )
                                        }
                                    }
                                },
                                onToggleWeeklyDay = { day ->
                                    val current = reminderRecurrence.weeklyDays
                                    val updated = if (day in current) current - day else current + day
                                    reminderRecurrence = reminderRecurrence.copy(weeklyDays = updated)
                                },
                            )
                            AffairType.OPPORTUNITY -> {
                                OpportunityPeriodSection(
                                    startMillis = startMillis.takeIf { hasStart },
                                    endMillis = endMillis.takeIf { hasEnd },
                                    onSetStartDateClick = {
                                        datePickTarget = AffairDatePickTarget.OPPORTUNITY_START
                                        showDatePicker = true
                                    },
                                    onSetStartTimeClick = {
                                        ensureOpportunityStartDefaults(
                                            zone = zone,
                                            hasStart = hasStart,
                                            startMillis = startMillis,
                                            onUpdate = { hasStart = true; startMillis = it },
                                        )
                                        timePickTarget = AffairTimePickTarget.OPPORTUNITY_START
                                        showTimePicker = true
                                    },
                                    onSetEndDateClick = {
                                        datePickTarget = AffairDatePickTarget.OPPORTUNITY_END
                                        showDatePicker = true
                                    },
                                    onSetEndTimeClick = {
                                        ensureOpportunityEndDefaults(
                                            zone = zone,
                                            hasStart = hasStart,
                                            hasEnd = hasEnd,
                                            startMillis = startMillis,
                                            endMillis = endMillis,
                                            onUpdateStart = { hasStart = true; startMillis = it },
                                            onUpdateEnd = { hasEnd = true; endMillis = it },
                                        )
                                        timePickTarget = AffairTimePickTarget.OPPORTUNITY_END
                                        showTimePicker = true
                                    },
                                    onClearStart = {
                                        hasStart = false
                                        startMillis = 0L
                                    },
                                    onClearEnd = {
                                        hasEnd = false
                                        endMillis = 0L
                                    },
                                    onClearStartTime = {
                                        if (!hasStart) return@OpportunityPeriodSection
                                        val date = localDateFromMillis(startMillis, zone)
                                        startMillis = startOfDayMillis(date, zone)
                                    },
                                    onClearEndTime = {
                                        if (!hasEnd) return@OpportunityPeriodSection
                                        val date = localDateFromMillis(endMillis, zone)
                                        endMillis = endOfDayMillis(date, zone)
                                    },
                                    onClearAll = {
                                        hasStart = false
                                        hasEnd = false
                                        startMillis = 0L
                                        endMillis = 0L
                                    },
                                )
                                CourseEditSectionDivider()
                                CourseEditColorSection(
                                    cardColor = cardColor,
                                    onPickColor = { showColorPicker = true },
                                    sectionTitle = "显示颜色",
                                )
                            }
                            AffairType.TASK -> Unit
                            AffairType.EVENT -> Unit
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = when (datePickTarget) {
            AffairDatePickTarget.OPPORTUNITY_END -> if (hasEnd) endMillis else startMillis.takeIf { hasStart }
            else -> startMillis.takeIf { hasStart }
        }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?.let { localDateToDatePickerUtcMillis(it) }
            ?: localDateToDatePickerUtcMillis(LocalDate.now(zone))

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        PoolDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = {
                val picked = datePickerState.selectedDateMillis ?: return@PoolDatePickerDialog
                val pickedDate = datePickerUtcMillisToLocalDate(picked)
                when (datePickTarget) {
                    AffairDatePickTarget.REMINDER_DAY -> {
                        startMillis = applyDateToBoundaryMillis(
                            pickedDate = pickedDate,
                            existingMillis = startMillis.takeIf { hasStart },
                            isEndBoundary = false,
                            zone = zone,
                        )
                        hasStart = true
                    }
                    AffairDatePickTarget.OPPORTUNITY_START -> {
                        startMillis = applyDateToBoundaryMillis(
                            pickedDate = pickedDate,
                            existingMillis = startMillis.takeIf { hasStart },
                            isEndBoundary = false,
                            zone = zone,
                        )
                        hasStart = true
                        normalizeOpportunityRange(
                            zone = zone,
                            hasStart = hasStart,
                            hasEnd = hasEnd,
                            startMillis = startMillis,
                            endMillis = endMillis,
                            onAdjustEnd = { endMillis = it },
                            onAdjustStart = { startMillis = it },
                        )
                    }
                    AffairDatePickTarget.OPPORTUNITY_END -> {
                        endMillis = applyDateToBoundaryMillis(
                            pickedDate = pickedDate,
                            existingMillis = endMillis.takeIf { hasEnd },
                            isEndBoundary = true,
                            zone = zone,
                        )
                        hasEnd = true
                        normalizeOpportunityRange(
                            zone = zone,
                            hasStart = hasStart,
                            hasEnd = hasEnd,
                            startMillis = startMillis,
                            endMillis = endMillis,
                            onAdjustEnd = { endMillis = it },
                            onAdjustStart = { startMillis = it },
                        )
                    }
                    AffairDatePickTarget.EVENT_DAY -> {
                        val startTime = if (hasStart) {
                            Instant.ofEpochMilli(startMillis).atZone(zone).toLocalTime()
                        } else {
                            LocalTime.of(12, 0)
                        }
                        val endTime = if (hasEnd) {
                            Instant.ofEpochMilli(endMillis).atZone(zone).toLocalTime()
                        } else {
                            startTime.plusHours(1)
                        }
                        startMillis = LocalDateTime.of(pickedDate, startTime)
                            .atZone(zone).toInstant().toEpochMilli()
                        hasStart = true
                        var resolvedEnd = LocalDateTime.of(pickedDate, endTime)
                            .atZone(zone).toInstant().toEpochMilli()
                        if (resolvedEnd <= startMillis) {
                            resolvedEnd = LocalDateTime.of(pickedDate, startTime.plusHours(1))
                                .atZone(zone).toInstant().toEpochMilli()
                        }
                        endMillis = resolvedEnd
                        hasEnd = true
                    }
                }
                showDatePicker = false
            },
            datePickerState = datePickerState,
        )
    }

    if (showTimePicker) {
        val baseMillis = when (timePickTarget) {
            AffairTimePickTarget.EVENT_START -> startMillis
            AffairTimePickTarget.EVENT_END -> if (hasEnd) endMillis else startMillis
            AffairTimePickTarget.OPPORTUNITY_START -> startMillis
            AffairTimePickTarget.OPPORTUNITY_END -> if (hasEnd) endMillis else startMillis
            AffairTimePickTarget.REMINDER -> startMillis
        }
        val current = Instant.ofEpochMilli(baseMillis).atZone(zone)
        val timePickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PoolColors.Block1,
            ) {
                Column(modifier = Modifier.background(PoolColors.Block1)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TimePicker(
                            state = timePickerState,
                            colors = PoolPickerDefaults.timePickerColors(),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("取消", color = PoolColors.TextPrimary)
                        }
                        TextButton(
                            onClick = {
                                val date = Instant.ofEpochMilli(baseMillis).atZone(zone).toLocalDate()
                                val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                                val pickedMillis = combineAffairDateAndTime(date, time, zone)
                                when (timePickTarget) {
                                    AffairTimePickTarget.EVENT_START -> {
                                        startMillis = pickedMillis
                                        hasStart = true
                                        if (!hasEnd || endMillis <= startMillis) {
                                            endMillis = LocalDateTime.of(date, time.plusHours(1))
                                                .atZone(zone).toInstant().toEpochMilli()
                                            hasEnd = true
                                        }
                                    }
                                    AffairTimePickTarget.EVENT_END -> {
                                        endMillis = pickedMillis
                                        hasEnd = true
                                        if (hasStart && endMillis <= startMillis) {
                                            startMillis = LocalDateTime.of(date, time.minusHours(1))
                                                .atZone(zone).toInstant().toEpochMilli()
                                            hasStart = true
                                        }
                                    }
                                    AffairTimePickTarget.OPPORTUNITY_START -> {
                                        startMillis = pickedMillis
                                        hasStart = true
                                        normalizeOpportunityRange(
                                            zone = zone,
                                            hasStart = hasStart,
                                            hasEnd = hasEnd,
                                            startMillis = startMillis,
                                            endMillis = endMillis,
                                            onAdjustEnd = { endMillis = it },
                                            onAdjustStart = { startMillis = it },
                                        )
                                    }
                                    AffairTimePickTarget.OPPORTUNITY_END -> {
                                        endMillis = pickedMillis
                                        hasEnd = true
                                        normalizeOpportunityRange(
                                            zone = zone,
                                            hasStart = hasStart,
                                            hasEnd = hasEnd,
                                            startMillis = startMillis,
                                            endMillis = endMillis,
                                            onAdjustEnd = { endMillis = it },
                                            onAdjustStart = { startMillis = it },
                                        )
                                    }
                                    AffairTimePickTarget.REMINDER -> {
                                        startMillis = pickedMillis
                                        hasStart = true
                                    }
                                }
                                showTimePicker = false
                            },
                        ) {
                            Text("确定", color = PoolColors.AccentPrimary)
                        }
                    }
                }
            }
        }
    }
}

private fun ensureReminderTimeDefaults(
    zone: ZoneId,
    hasStart: Boolean,
    startMillis: Long,
    onUpdate: (Long) -> Unit,
) {
    if (hasStart) return
    val today = LocalDate.now(zone)
    onUpdate(today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli())
}

private fun ensureOpportunityStartDefaults(
    zone: ZoneId,
    hasStart: Boolean,
    startMillis: Long,
    onUpdate: (Long) -> Unit,
) {
    if (hasStart) return
    val today = LocalDate.now(zone)
    onUpdate(startOfDayMillis(today, zone))
}

private fun ensureOpportunityEndDefaults(
    zone: ZoneId,
    hasStart: Boolean,
    hasEnd: Boolean,
    startMillis: Long,
    endMillis: Long,
    onUpdateStart: (Long) -> Unit,
    onUpdateEnd: (Long) -> Unit,
) {
    if (hasEnd) return
    if (hasStart) {
        val date = localDateFromMillis(startMillis, zone)
        onUpdateEnd(endOfDayMillis(date, zone))
        return
    }
    val today = LocalDate.now(zone)
    onUpdateStart(startOfDayMillis(today, zone))
    onUpdateEnd(endOfDayMillis(today, zone))
}

private fun normalizeOpportunityRange(
    zone: ZoneId,
    hasStart: Boolean,
    hasEnd: Boolean,
    startMillis: Long,
    endMillis: Long,
    onAdjustEnd: (Long) -> Unit,
    onAdjustStart: (Long) -> Unit,
) {
    if (!hasStart || !hasEnd || endMillis >= startMillis) return
    val startDate = localDateFromMillis(startMillis, zone)
    val adjustedEnd = when {
        isAffairEndDateOnly(endMillis, zone) -> endOfDayMillis(startDate, zone)
        isAffairStartDateOnly(startMillis, zone) -> endOfDayMillis(startDate, zone)
        else -> combineAffairDateAndTime(startDate, localTimeFromMillis(endMillis, zone), zone)
            .coerceAtLeast(startMillis)
    }
    onAdjustEnd(adjustedEnd)
    if (adjustedEnd < startMillis) {
        onAdjustStart(startMillis)
    }
}

private fun ensureEventStartDefaults(
    zone: ZoneId,
    hasStart: Boolean,
    startMillis: Long,
    onUpdateStart: (Long) -> Unit,
) {
    if (hasStart) return
    val today = LocalDate.now(zone)
    onUpdateStart(today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli())
}

private fun ensureEventEndDefaults(
    zone: ZoneId,
    hasStart: Boolean,
    hasEnd: Boolean,
    startMillis: Long,
    onUpdateStart: (Long) -> Unit,
    onUpdateEnd: (Long) -> Unit,
) {
    val effectiveStart = if (hasStart) {
        startMillis
    } else {
        val default = LocalDate.now(zone).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        onUpdateStart(default)
        default
    }
    if (!hasEnd) {
        val startTime = Instant.ofEpochMilli(effectiveStart).atZone(zone).toLocalTime()
        val date = Instant.ofEpochMilli(effectiveStart).atZone(zone).toLocalDate()
        onUpdateEnd(
            LocalDateTime.of(date, startTime.plusHours(1)).atZone(zone).toInstant().toEpochMilli(),
        )
    }
}

private fun buildAffairEntity(
    affairType: AffairType,
    affairId: Long,
    title: String,
    note: String?,
    hasStart: Boolean,
    startMillis: Long,
    hasEnd: Boolean,
    endMillis: Long,
    location: String?,
    cardColor: Long? = null,
    reminderRecurrence: ReminderRecurrence = ReminderRecurrence(),
): AffairEntity = when (affairType) {
    AffairType.REMINDER -> AffairEntity(
        id = affairId,
        type = affairType,
        title = title,
        startAt = if (hasStart) startMillis else null,
        recurrenceRule = reminderRecurrence.encode(),
        note = note,
    )
    AffairType.OPPORTUNITY -> AffairEntity(
        id = affairId,
        type = affairType,
        title = title,
        startAt = if (hasStart) startMillis else null,
        endAt = if (hasEnd) endMillis else null,
        note = note,
        cardColor = cardColor,
    )
    AffairType.EVENT -> AffairEntity(
        id = affairId,
        type = affairType,
        title = title,
        startAt = if (hasStart) startMillis else null,
        endAt = if (hasEnd) endMillis else null,
        location = location,
        cardColor = cardColor,
        note = note,
    )
    AffairType.TASK -> AffairEntity(
        id = affairId,
        type = affairType,
        title = title,
        note = note,
    )
}
