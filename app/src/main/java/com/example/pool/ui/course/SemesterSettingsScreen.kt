package com.example.pool.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pool.data.schedule.Semester
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDatePickerDialog
import com.example.pool.ui.components.PoolRowHorizontalPadding
import com.example.pool.ui.course.components.CourseEditActionText
import com.example.pool.ui.course.components.CourseEditSectionDivider
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.SemesterCalendar
import com.example.pool.util.datePickerUtcMillisToLocalDate
import com.example.pool.util.localDateToDatePickerUtcMillis
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterSettingsScreen(
    viewModel: CourseViewModel,
    onBack: () -> Unit,
) {
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    var editingSemester by remember { mutableStateOf<Semester?>(null) }
    var showNewSemester by remember { mutableStateOf(false) }

    if (editingSemester != null || showNewSemester) {
        SemesterEditDialog(
            semester = editingSemester,
            onDismiss = {
                editingSemester = null
                showNewSemester = false
            },
            onSave = { semester ->
                viewModel.upsertSemester(semester)
                editingSemester = null
                showNewSemester = false
            },
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("学期设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = PoolColors.AccentPrimary,
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
            PoolBlock1(modifier = Modifier.padding(top = 8.dp)) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PoolRowHorizontalPadding, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "学期列表", style = ScheduleFormStyles.sectionTitle)
                        Spacer(modifier = Modifier.weight(1f))
                        CourseEditActionText(
                            text = "添加学期",
                            leadingIcon = Icons.Default.Add,
                            onClick = { showNewSemester = true },
                        )
                    }

                    if (semesters.isEmpty()) {
                        Text(
                            text = "暂无学期，请添加",
                            style = ScheduleFormStyles.secondary,
                            modifier = Modifier
                                .padding(horizontal = PoolRowHorizontalPadding)
                                .padding(bottom = 16.dp),
                        )
                    } else {
                        semesters.forEachIndexed { index, semester ->
                            SemesterRow(
                                semester = semester,
                                onSelect = { viewModel.setActiveSemester(semester.id) },
                                onEdit = { editingSemester = semester },
                            )
                            if (index < semesters.lastIndex) {
                                CourseEditSectionDivider()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SemesterRow(
    semester: Semester,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    val startDate = LocalDate.ofEpochDay(semester.startDateEpochDay)
    val weekCount = SemesterCalendar.weekCount(semester)
    val rowBackground = if (semester.isActive) ScheduleFormStyles.cardBackground else androidx.compose.ui.graphics.Color.Transparent
    val rowShape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PoolRowHorizontalPadding, vertical = 4.dp)
            .background(rowBackground, rowShape)
            .clickable(onClick = onEdit)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = semester.isActive,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = PoolColors.AccentPrimary,
                unselectedColor = PoolColors.TextSecondary,
            ),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = semester.name, style = ScheduleFormStyles.body)
            Text(
                text = "共 $weekCount 周 · 第1周周一 ${startDate.monthValue}月${startDate.dayOfMonth}日",
                style = ScheduleFormStyles.secondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        CourseEditActionText(text = "编辑", onClick = onEdit)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterEditDialog(
    semester: Semester?,
    onDismiss: () -> Unit,
    onSave: (Semester) -> Unit,
) {
    val isNew = semester == null
    var name by remember { mutableStateOf(semester?.name.orEmpty()) }
    var startDate by remember {
        mutableStateOf(
            semester?.let { SemesterCalendar.startDate(it) }
                ?: SemesterCalendar.normalizedSemesterStart(LocalDate.now()),
        )
    }
    var weekCount by remember {
        mutableIntStateOf(
            semester?.let { SemesterCalendar.weekCount(it) } ?: 18,
        )
    }
    var setActive by remember { mutableStateOf(semester?.isActive ?: true) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    val datesValid = weekCount in 1..26

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToDatePickerUtcMillis(startDate),
        )
        PoolDatePickerDialog(
            onDismiss = { showStartDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { millis ->
                    startDate = SemesterCalendar.normalizedSemesterStart(
                        datePickerUtcMillisToLocalDate(millis),
                    )
                }
                showStartDatePicker = false
            },
            datePickerState = datePickerState,
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PoolColors.Block1,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    text = if (isNew) "添加学期" else "编辑学期",
                    style = ScheduleFormStyles.sectionTitle.copy(fontSize = 16.sp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "学期名称", style = ScheduleFormStyles.sectionTitle)
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = ScheduleFormStyles.input,
                    singleLine = true,
                    cursorBrush = SolidColor(PoolColors.Accent),
                    decorationBox = { inner ->
                        if (name.isEmpty()) {
                            Text(text = "如 2026秋季学期", style = ScheduleFormStyles.placeholder)
                        }
                        inner()
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "第 1 周周一", style = ScheduleFormStyles.sectionTitle)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${startDate.year}年${startDate.monthValue}月${startDate.dayOfMonth}日",
                            style = ScheduleFormStyles.body,
                        )
                    }
                    CourseEditActionText(
                        text = "选择日期",
                        onClick = { showStartDatePicker = true },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "教学周数", style = ScheduleFormStyles.sectionTitle)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "共 $weekCount 周", style = ScheduleFormStyles.body)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (weekCount > 1) weekCount-- },
                            enabled = weekCount > 1,
                        ) {
                            Text("−", style = ScheduleFormStyles.sectionTitle.copy(fontSize = 20.sp))
                        }
                        IconButton(
                            onClick = { if (weekCount < 26) weekCount++ },
                            enabled = weekCount < 26,
                        ) {
                            Text("+", style = ScheduleFormStyles.sectionTitle.copy(fontSize = 20.sp))
                        }
                    }
                }
                if (!datesValid) {
                    Text(
                        text = "学期长度应在 1–26 周内",
                        color = MaterialTheme.colorScheme.error,
                        style = ScheduleFormStyles.secondary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = setActive,
                        onClick = { setActive = true },
                        colors = RadioButtonDefaults.colors(selectedColor = PoolColors.AccentPrimary),
                    )
                    Text(text = "设为当前学期", style = ScheduleFormStyles.secondary)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = PoolColors.TextPrimary)
                    }
                    TextButton(
                        onClick = {
                            if (name.isBlank() || !datesValid) return@TextButton
                            val normalizedStart = SemesterCalendar.normalizedSemesterStart(startDate)
                            val end = SemesterCalendar.endDateForWeekCount(normalizedStart, weekCount)
                            onSave(
                                Semester(
                                    id = semester?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    startDateEpochDay = normalizedStart.toEpochDay(),
                                    endDateEpochDay = end.toEpochDay(),
                                    isActive = setActive,
                                ),
                            )
                        },
                        enabled = name.isNotBlank() && datesValid,
                    ) {
                        Text(
                            "保存",
                            color = if (name.isNotBlank() && datesValid) {
                                PoolColors.AccentPrimary
                            } else {
                                PoolColors.TextSecondary
                            },
                        )
                    }
                }
            }
        }
    }
}
