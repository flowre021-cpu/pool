package com.example.pool.ui.course

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolConfirmDialog
import com.example.pool.ui.course.components.CourseColorPickerDialog
import com.example.pool.ui.course.components.CourseEditColorSection
import com.example.pool.ui.course.components.CourseEditNoteSection
import com.example.pool.ui.course.components.CourseEditSectionDivider
import com.example.pool.ui.course.components.CourseEditTextFieldSection
import com.example.pool.ui.course.components.CourseEditTimePeriodsSection
import com.example.pool.ui.course.model.CourseFormState
import com.example.pool.ui.course.model.CourseTimePeriod
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.DEFAULT_WEEK_COUNT
import com.example.pool.util.SemesterCalendar
import com.example.pool.util.isContiguousFullWeekSelection
import com.example.pool.util.syncSelectedWeeksForSemesterLength
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditScreen(
    courseId: Long,
    prefilledDay: Int,
    prefilledSection: Int,
    scheduleRepository: ScheduleRepository,
    onBack: () -> Unit,
) {
    val viewModel: CourseViewModel = viewModel(factory = CourseViewModelFactory(scheduleRepository))
    val context = LocalContext.current
    val colorPrefs = remember { CardColorPreferences(context) }
    val courseGroups by viewModel.courseGroups.collectAsStateWithLifecycle()
    val timeSlotEntities by viewModel.timeSlots.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val maxSection = timeSlotEntities.maxOfOrNull { it.sectionNumber } ?: 12
    val totalWeeks = activeSemester?.let { SemesterCalendar.weekCount(it) } ?: DEFAULT_WEEK_COUNT

    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var cardColor by remember { mutableStateOf(SchedulePastelColors.default) }
    var credits by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var groupId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var isFormLoaded by remember(courseId) { mutableStateOf(courseId == 0L) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var syncedTotalWeeks by remember(courseId) { mutableIntStateOf(-1) }
    var timePeriods by remember(courseId) {
        val day = if (prefilledDay in 1..7) prefilledDay else 1
        val section = if (prefilledSection > 0) prefilledSection else 1
        val initialWeeks = activeSemester?.let { SemesterCalendar.weekCount(it) } ?: DEFAULT_WEEK_COUNT
        mutableStateOf(
            listOf(
                CourseTimePeriod(
                    selectedWeeks = (1..initialWeeks).toList(),
                    dayOfWeek = day,
                    startSection = section,
                    endSection = section,
                ),
            ),
        )
    }
    var showColorPicker by remember { mutableStateOf(false) }
    var hasSuggestedColor by remember(courseId) { mutableStateOf(courseId != 0L) }

    LaunchedEffect(courseId, courseGroups) {
        if (courseId == 0L && !hasSuggestedColor) {
            cardColor = viewModel.suggestColorForNewGroup(colorPrefs.resolveActiveColors())
            hasSuggestedColor = true
        }
    }

    LaunchedEffect(courseId) {
        if (courseId != 0L) {
            val form = viewModel.loadCourseForm(courseId)
            if (form != null) {
                name = form.name
                cardColor = form.cardColor
                credits = form.credits
                note = form.note
                groupId = form.groupId
                timePeriods = form.timePeriods.map { period ->
                    period.copy(selectedWeeks = period.selectedWeeks.filter { it in 1..totalWeeks })
                }
                location = form.timePeriods.firstOrNull()?.location.orEmpty()
                teacher = form.timePeriods.firstOrNull()?.teacher.orEmpty()
                syncedTotalWeeks = -1
                isFormLoaded = true
            }
        }
    }

    LaunchedEffect(totalWeeks, isFormLoaded) {
        if (!isFormLoaded) return@LaunchedEffect
        when {
            syncedTotalWeeks < 0 -> {
                timePeriods = timePeriods.map { period ->
                    val selected = period.selectedWeeks.filter { it in 1..totalWeeks }
                    val maxWeek = selected.maxOrNull() ?: 0
                    val next = if (isContiguousFullWeekSelection(selected) && maxWeek < totalWeeks) {
                        (1..totalWeeks).toList()
                    } else {
                        selected
                    }
                    period.copy(selectedWeeks = next)
                }
                syncedTotalWeeks = totalWeeks
            }
            totalWeeks > syncedTotalWeeks -> {
                timePeriods = timePeriods.map { period ->
                    period.copy(
                        selectedWeeks = syncSelectedWeeksForSemesterLength(
                            selectedWeeks = period.selectedWeeks,
                            previousTotal = syncedTotalWeeks,
                            newTotal = totalWeeks,
                        ),
                    )
                }
                syncedTotalWeeks = totalWeeks
            }
            totalWeeks < syncedTotalWeeks -> {
                timePeriods = timePeriods.map { period ->
                    period.copy(selectedWeeks = period.selectedWeeks.filter { it in 1..totalWeeks })
                }
                syncedTotalWeeks = totalWeeks
            }
        }
    }

    val canSave = isFormLoaded && name.isNotBlank()

    if (showColorPicker) {
        CourseColorPickerDialog(
            selected = cardColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { cardColor = it },
        )
    }

    if (showDeleteConfirm) {
        PoolConfirmDialog(
            title = "删除课程",
            message = "将删除「$name」的全部上课时段，此操作不可撤销。",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteCourseGroup(groupId, onBack)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (courseId == 0L) "添加课程" else "编辑课程")
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
                    TextButton(
                        onClick = {
                            if (!canSave) return@TextButton
                            val syncedPeriods = timePeriods.map {
                                it.copy(
                                    location = location.trim(),
                                    teacher = teacher.trim(),
                                )
                            }
                            val error = validateCourseForm(syncedPeriods, maxSection)
                            if (error != null) {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            val form = CourseFormState(
                                groupId = groupId,
                                name = name.trim(),
                                cardColor = cardColor,
                                credits = credits.trim(),
                                note = note.trim(),
                                timePeriods = syncedPeriods,
                            )
                            viewModel.saveCourseForm(form, onBack)
                        },
                        enabled = canSave,
                    ) {
                        Text("保存", color = if (canSave) PoolColors.Accent else PoolColors.TextSecondary)
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
                    CourseEditTextFieldSection(
                        title = "课程名称",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        value = name,
                        onValueChange = { name = it },
                    )

                    CourseEditSectionDivider()

                    CourseEditTextFieldSection(
                        title = "上课地点",
                        icon = Icons.Default.Place,
                        value = location,
                        onValueChange = { location = it },
                        placeholder = "（可不填）",
                    )

                    CourseEditSectionDivider()

                    CourseEditTimePeriodsSection(
                        timePeriods = timePeriods,
                        maxSection = maxSection,
                        totalWeeks = totalWeeks,
                        onAddPeriod = {
                            val inheritDay = timePeriods.firstOrNull()?.dayOfWeek ?: 1
                            timePeriods = timePeriods + CourseTimePeriod(
                                selectedWeeks = (1..totalWeeks).toList(),
                                dayOfWeek = inheritDay,
                                location = location.trim(),
                                teacher = teacher.trim(),
                            )
                        },
                        onUpdatePeriod = { index, updated ->
                            timePeriods = timePeriods.toMutableList().also { it[index] = updated }
                        },
                        onRemovePeriod = { index ->
                            timePeriods = timePeriods.filterIndexed { i, _ -> i != index }
                        },
                    )

                    CourseEditSectionDivider()

                    CourseEditNoteSection(
                        value = note,
                        onValueChange = { note = it },
                        helperText = "整体备注，将显示在课表卡片上（所有时间段共用）",
                    )

                    CourseEditSectionDivider()

                    CourseEditTextFieldSection(
                        title = "授课老师",
                        icon = Icons.Default.Person,
                        value = teacher,
                        onValueChange = { teacher = it },
                        placeholder = "（可不填）",
                    )

                    CourseEditSectionDivider()

                    CourseEditTextFieldSection(
                        title = "学分",
                        icon = Icons.Default.School,
                        value = credits,
                        onValueChange = { credits = it },
                        placeholder = "（可不填）",
                    )

                    CourseEditSectionDivider()

                    CourseEditColorSection(
                        cardColor = cardColor,
                        onPickColor = { showColorPicker = true },
                    )

                    if (courseId != 0L) {
                        CourseEditSectionDivider()
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                        ) {
                            Text("删除课程", color = PoolColors.DeleteRed)
                        }
                    }
                }
            }
        }
    }
}

private fun validateCourseForm(timePeriods: List<CourseTimePeriod>, maxSection: Int): String? {
    if (timePeriods.isEmpty()) return "请至少添加一个时间段"
    timePeriods.forEach { period ->
        if (period.selectedWeeks.isEmpty()) return "请至少选择一个上课周次"
        if (period.startSection !in 1..maxSection ||
            period.endSection !in 1..maxSection ||
            period.endSection < period.startSection
        ) {
            return "请输入有效的上课节次"
        }
    }
    return null
}
