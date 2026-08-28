package com.example.pool.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import com.example.pool.data.PlannerRepository
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDatePickerDialog
import com.example.pool.ui.components.PoolPickerDefaults
import com.example.pool.ui.course.components.CourseEditSectionDivider
import com.example.pool.ui.task.components.TaskEditDdlSection
import com.example.pool.ui.task.components.TaskEditNoteSection
import com.example.pool.ui.task.components.TaskEditTextFieldSection
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.datePickerUtcMillisToLocalDate
import com.example.pool.util.localDateToDatePickerUtcMillis
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: Long,
    repository: PlannerRepository,
    onBack: () -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: TaskViewModel = viewModel(factory = TaskViewModelFactory(repository, appContext))
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var deadlineMillis by remember { mutableLongStateOf(0L) }
    var hasDeadline by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId != 0L) {
            val task = viewModel.getTask(taskId)
            if (task != null) {
                title = task.title
                note = task.note.orEmpty()
                if (task.deadline != null) {
                    hasDeadline = true
                    deadlineMillis = task.deadline
                }
            }
        }
    }

    val canSave = title.isNotBlank()
    val context = LocalContext.current

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (taskId == 0L) "新建待办" else "编辑待办")
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
                            if (title.isBlank()) {
                                Toast.makeText(context, "请输入标题", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            viewModel.saveTask(
                                id = taskId,
                                title = title.trim(),
                                deadline = if (hasDeadline) deadlineMillis else null,
                                note = note.trim().ifBlank { null },
                                onSaved = onBack,
                            )
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

                    TaskEditDdlSection(
                        hasDeadline = hasDeadline,
                        deadlineMillis = deadlineMillis,
                        onSetDateClick = { showDatePicker = true },
                        onSetTimeClick = {
                            if (!hasDeadline) {
                                val today = LocalDate.now()
                                deadlineMillis = LocalDateTime.of(today, LocalTime.of(23, 59))
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                hasDeadline = true
                            }
                            showTimePicker = true
                        },
                        onClear = {
                            hasDeadline = false
                            deadlineMillis = 0L
                        },
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val initialDateMillis = when {
            hasDeadline -> Instant.ofEpochMilli(deadlineMillis)
                .atZone(ZoneId.systemDefault()).toLocalDate()
                .let { localDateToDatePickerUtcMillis(it) }
            else -> localDateToDatePickerUtcMillis(LocalDate.now())
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        PoolDatePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = {
                val selectedDate = datePickerState.selectedDateMillis
                if (selectedDate != null) {
                    val current = if (hasDeadline) {
                        Instant.ofEpochMilli(deadlineMillis).atZone(ZoneId.systemDefault())
                    } else {
                        LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                    }
                    val pickedDate = datePickerUtcMillisToLocalDate(selectedDate)
                    val time = if (hasDeadline) current.toLocalTime() else LocalTime.of(23, 59)
                    deadlineMillis = LocalDateTime.of(pickedDate, time)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    hasDeadline = true
                }
                showDatePicker = false
            },
            datePickerState = datePickerState,
        )
    }

    if (showTimePicker) {
        val zone = ZoneId.systemDefault()
        val current = Instant.ofEpochMilli(deadlineMillis).atZone(zone)
        val timePickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = RoundedCornerShape(16.dp),
                color = PoolColors.Block1,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
            ) {
                Column(modifier = Modifier.background(PoolColors.Block1)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PoolColors.Block1)
                            .padding(top = 48.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        TimePicker(
                            state = timePickerState,
                            colors = PoolPickerDefaults.timePickerColors(),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PoolColors.Block1)
                            .padding(end = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("取消", color = PoolColors.TextPrimary)
                        }
                        TextButton(
                            onClick = {
                                val date = Instant.ofEpochMilli(deadlineMillis).atZone(zone).toLocalDate()
                                val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                                deadlineMillis = LocalDateTime.of(date, time)
                                    .atZone(zone)
                                    .toInstant()
                                    .toEpochMilli()
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
