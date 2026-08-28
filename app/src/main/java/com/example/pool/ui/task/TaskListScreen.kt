package com.example.pool.ui.task

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pool.data.AffairEntity
import com.example.pool.data.PlannerRepository
import com.example.pool.ui.components.PoolSelectionTopBarActions
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.DeadlineStatus
import com.example.pool.util.DeadlineUrgency
import com.example.pool.util.deadlineColor
import com.example.pool.util.deadlineStatus

private val TaskCheckboxWidth = 48.dp
private val TaskContentTopPadding = 14.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    repository: PlannerRepository,
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: TaskViewModel = viewModel(factory = TaskViewModelFactory(repository, appContext))
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var pendingDeleteTasks by remember { mutableStateOf<List<AffairEntity>?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf(setOf<Long>()) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedTaskIds = emptySet()
    }

    pendingDeleteTasks?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTasks = null },
            containerColor = PoolColors.Block1,
            titleContentColor = PoolColors.TextPrimary,
            textContentColor = PoolColors.TextPrimary,
            title = { Text("确认删除") },
            text = { Text("确定删除选中的 ${toDelete.size} 项任务吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTasks(toDelete)
                        exitSelectionMode()
                        pendingDeleteTasks = null
                    },
                ) {
                    Text("删除", color = PoolColors.DeleteRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTasks = null }) {
                    Text("取消", color = PoolColors.TextPrimary)
                }
            },
            tonalElevation = AlertDialogDefaults.TonalElevation,
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (selectionMode) "已选择 ${selectedTaskIds.size} 项" else "Task")
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectionMode) exitSelectionMode() else onBack() }) {
                        Icon(
                            if (selectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (selectionMode) "取消选择" else "返回",
                            tint = PoolColors.AccentPrimary,
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        PoolSelectionTopBarActions(
                            selectedCount = selectedTaskIds.size,
                            onDelete = {
                                val selected = tasks.filter { it.id in selectedTaskIds }
                                if (selected.isNotEmpty()) pendingDeleteTasks = selected
                            },
                        )
                    } else {
                        IconButton(onClick = onAddTask) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "添加任务",
                                tint = PoolColors.Accent,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PoolColors.NavBar,
                    titleContentColor = PoolColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("暂无任务，点右上角 + 添加", color = PoolColors.TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        selectionMode = selectionMode,
                        selected = task.id in selectedTaskIds,
                        onToggleDone = { if (!selectionMode) viewModel.toggleDone(task) },
                        onClick = {
                            if (selectionMode) {
                                selectedTaskIds = if (task.id in selectedTaskIds) {
                                    selectedTaskIds - task.id
                                } else {
                                    selectedTaskIds + task.id
                                }
                                if (selectedTaskIds.isEmpty()) selectionMode = false
                            } else {
                                onEditTask(task.id)
                            }
                        },
                        onLongClick = {
                            if (selectionMode) {
                                selectedTaskIds = if (task.id in selectedTaskIds) {
                                    selectedTaskIds - task.id
                                } else {
                                    selectedTaskIds + task.id
                                }
                                if (selectedTaskIds.isEmpty()) selectionMode = false
                            } else {
                                selectionMode = true
                                selectedTaskIds = selectedTaskIds + task.id
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    task: AffairEntity,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val status = deadlineStatus(task.deadline, task.isDone)
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        shape = RoundedCornerShape(10.dp),
        color = PoolColors.Block1,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(TaskCheckboxWidth)
                    .padding(top = 4.dp),
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PoolColors.AccentPrimary,
                            uncheckedColor = PoolColors.TextSecondary,
                            checkmarkColor = PoolColors.Block1,
                        ),
                    )
                } else {
                    Checkbox(
                        checked = task.isDone,
                        onCheckedChange = { onToggleDone() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PoolColors.AccentPrimary,
                            uncheckedColor = PoolColors.TextSecondary,
                            checkmarkColor = PoolColors.Block1,
                        ),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = TaskContentTopPadding),
            ) {
                Text(
                    text = task.title,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isDone) PoolColors.TextSecondary else PoolColors.TextPrimary,
                    ),
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                )
                TaskDdlLine(status = status, isDone = task.isDone)
            }
        }
    }
}

@Composable
private fun TaskDdlLine(
    status: DeadlineStatus,
    isDone: Boolean,
) {
    val color = if (isDone) PoolColors.TextSecondary else deadlineColor(status.urgency)
    val isExpired = !isDone && status.urgency == DeadlineUrgency.OVERDUE

    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = status.timeText,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = color,
            ),
        )
        if (isExpired) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = "[",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = color,
                    ),
                )
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = " 已过期]",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = color,
                    ),
                )
            }
        } else if (!isDone && status.badgeText != null) {
            Text(
                text = "[${status.badgeText}]",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = color,
                ),
            )
        }
    }
}
