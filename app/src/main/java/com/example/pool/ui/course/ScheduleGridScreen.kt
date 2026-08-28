package com.example.pool.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.data.schedule.ScheduleTimeSlot
import com.example.pool.ui.components.PoolDatePickerDialog
import com.example.pool.ui.course.components.EditSingleTimeSlotDialog
import com.example.pool.ui.course.components.ScheduleEmptyCellKey
import com.example.pool.ui.course.components.ScheduleScrollableContent
import com.example.pool.ui.course.model.Course
import com.example.pool.ui.course.model.TimeSlot
import com.example.pool.ui.course.model.toUiCourse
import com.example.pool.ui.course.model.toUiTimeSlot
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.SemesterCalendar
import com.example.pool.util.datePickerUtcMillisToLocalDate
import com.example.pool.util.formatTodayHeader
import com.example.pool.util.localDateToDatePickerUtcMillis
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleGridScreen(
    scheduleRepository: ScheduleRepository,
    onBack: () -> Unit,
    onAddCourse: (dayOfWeek: Int?, startSection: Int?) -> Unit,
    onEditCourse: (courseId: Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: CourseViewModel = viewModel(
        factory = CourseViewModelFactory(scheduleRepository),
    )
    val courseEntities by viewModel.courses.collectAsStateWithLifecycle()
    val timeSlotEntities by viewModel.timeSlots.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()

    var currentWeek by remember { mutableIntStateOf(1) }
    var editingTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var highlightedEmptyCell by remember { mutableStateOf<ScheduleEmptyCellKey?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val semesterStart = remember(activeSemester) {
        activeSemester?.let { SemesterCalendar.startDate(it) }
    }
    val semesterWeekCount = remember(activeSemester) {
        activeSemester?.let { SemesterCalendar.weekCount(it) } ?: 1
    }
    val todayWeek = remember(activeSemester) {
        viewModel.currentWeekForToday(activeSemester)
    }

    LaunchedEffect(activeSemester) {
        currentWeek = viewModel.currentWeekForToday(activeSemester)
    }

    val timeSlots = remember(timeSlotEntities) { timeSlotEntities.map { it.toUiTimeSlot() } }
    val weekDateList = remember(currentWeek, semesterStart) {
        semesterStart?.let { SemesterCalendar.weekDates(currentWeek, it) }.orEmpty()
    }
    val visibleCourses = remember(courseEntities, currentWeek, activeSemester) {
        courseEntities
            .filter { course ->
                activeSemester == null ||
                    course.semesterId == null ||
                    course.semesterId == activeSemester?.id
            }
            .filter { currentWeek in it.selectedWeeks }
            .map { it.toUiCourse() }
    }

    val weekTitle = remember(currentWeek, activeSemester) {
        activeSemester?.let { semester ->
            SemesterCalendar.formatWeekTitle(
                weekNumber = currentWeek,
                semesterStart = SemesterCalendar.startDate(semester),
                semesterName = semester.name,
            )
        } ?: "第 $currentWeek 周"
    }

    val datePickerInitialDate = remember(currentWeek, weekDateList) {
        val today = LocalDate.now()
        weekDateList.firstOrNull { it == today }
            ?: weekDateList.firstOrNull()
            ?: today
    }

    if (showDatePicker && semesterStart != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToDatePickerUtcMillis(datePickerInitialDate),
        )
        PoolDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = {
                val pickedMillis = datePickerState.selectedDateMillis ?: return@PoolDatePickerDialog
                val pickedDate = datePickerUtcMillisToLocalDate(pickedMillis)
                currentWeek = SemesterCalendar.weekNumberForDate(pickedDate, semesterStart)
                    .coerceIn(1, semesterWeekCount)
                showDatePicker = false
            },
            datePickerState = datePickerState,
        )
    }

    editingTimeSlot?.let { slot ->
        EditSingleTimeSlotDialog(
            slot = slot,
            onDismiss = { editingTimeSlot = null },
            onSave = { updated ->
                viewModel.updateTimeSlot(
                    ScheduleTimeSlot(
                        sectionNumber = updated.sectionNumber,
                        startTimeMinutes = updated.startTimeMinutes,
                        endTimeMinutes = updated.endTimeMinutes,
                    ),
                )
                editingTimeSlot = null
            },
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable(enabled = semesterStart != null) {
                            showDatePicker = true
                        },
                    ) {
                        Text(
                            text = weekTitle,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (currentWeek != todayWeek) {
                            Text(
                                text = "回到今天",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = PoolColors.AccentPrimary,
                                modifier = Modifier.clickable {
                                    currentWeek = todayWeek
                                },
                            )
                        } else {
                            Text(
                                text = formatTodayHeader(),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = PoolColors.TextSecondary,
                            )
                        }
                    }
                },
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
                    IconButton(onClick = { if (currentWeek > 1) currentWeek-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一周", tint = PoolColors.Accent)
                    }
                    IconButton(
                        onClick = {
                            if (currentWeek < semesterWeekCount) currentWeek++
                        },
                        enabled = currentWeek < semesterWeekCount,
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一周", tint = PoolColors.Accent)
                    }
                    IconButton(onClick = { onAddCourse(null, null) }) {
                        Icon(Icons.Default.Add, contentDescription = "添加课程", tint = PoolColors.Accent)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置", tint = PoolColors.Accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PoolColors.NavBar,
                    titleContentColor = PoolColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        when {
            weekDateList.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    Text("请先在设置中添加并选择当前学期", color = PoolColors.TextSecondary)
                }
            }
            timeSlots.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    Text("节次时间未配置，请前往设置", color = PoolColors.TextSecondary)
                }
            }
            else -> {
                ScheduleScrollableContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    timeSlots = timeSlots,
                    courses = visibleCourses,
                    weekDates = weekDateList,
                    highlightedEmptyCell = highlightedEmptyCell,
                    onEmptyCellClick = { day, section ->
                        if (highlightedEmptyCell != null) return@ScheduleScrollableContent
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        highlightedEmptyCell = ScheduleEmptyCellKey(day, section)
                        scope.launch {
                            delay(260)
                            highlightedEmptyCell = null
                            onAddCourse(day, section)
                        }
                    },
                    onCourseClick = { course -> onEditCourse(course.id) },
                    onSectionClick = { slot -> editingTimeSlot = slot },
                )
            }
        }
    }
}
