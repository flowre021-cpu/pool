package com.example.pool.ui.course

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pool.data.schedule.CourseGroupSummary
import com.example.pool.data.schedule.Semester
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolConfirmDialog
import com.example.pool.ui.components.PoolDivider
import com.example.pool.ui.components.PoolRowHorizontalPadding
import com.example.pool.ui.components.PoolSelectionTopBarActions
import com.example.pool.ui.course.components.CourseEditActionText
import com.example.pool.ui.course.components.CourseEditSectionDivider
import com.example.pool.ui.theme.PoolColors

private const val LEGACY_SEMESTER_KEY = "__legacy__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseManagementScreen(
    viewModel: CourseViewModel,
    onBack: () -> Unit,
    onEditCourse: (Long) -> Unit,
) {
    val courseGroups by viewModel.courseGroups.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    var pendingDeleteCount by remember { mutableStateOf(0) }
    var expandedKeys by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedGroupIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    LaunchedEffect(activeSemester?.id, courseGroups) {
        if (expandedKeys.isEmpty() && courseGroups.isNotEmpty()) {
            val defaultKey = activeSemester?.id
                ?: courseGroups.firstNotNullOfOrNull { it.semesterId }
                ?: LEGACY_SEMESTER_KEY
            expandedKeys = setOf(defaultKey)
        }
    }

    val sections = remember(courseGroups, semesters) {
        groupCourseGroupsBySemester(courseGroups, semesters)
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectedGroupIds = emptySet()
    }

    if (pendingDeleteCount > 0) {
        PoolConfirmDialog(
            title = "删除课程",
            message = "将删除选中的 $pendingDeleteCount 门课程及其全部上课时段，此操作不可撤销。",
            onConfirm = {
                val ids = selectedGroupIds.toList()
                viewModel.deleteCourseGroups(ids) {
                    exitSelectionMode()
                }
                pendingDeleteCount = 0
            },
            onDismiss = { pendingDeleteCount = 0 },
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) "已选择 ${selectedGroupIds.size} 项" else "课程管理",
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectionMode) exitSelectionMode() else onBack()
                        },
                    ) {
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
                            selectedCount = selectedGroupIds.size,
                            onDelete = { pendingDeleteCount = selectedGroupIds.size },
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
                    if (sections.isEmpty()) {
                        Text(
                            text = "暂无课程",
                            style = ScheduleFormStyles.secondary,
                            modifier = Modifier
                                .padding(horizontal = PoolRowHorizontalPadding, vertical = 16.dp),
                        )
                    } else {
                        sections.forEachIndexed { sectionIndex, section ->
                            SemesterCourseSection(
                                title = section.title,
                                count = section.groups.size,
                                expanded = section.key in expandedKeys,
                                onToggle = {
                                    expandedKeys = if (section.key in expandedKeys) {
                                        expandedKeys - section.key
                                    } else {
                                        expandedKeys + section.key
                                    }
                                },
                                groups = section.groups,
                                selectionMode = selectionMode,
                                selectedGroupIds = selectedGroupIds,
                                onEnterSelection = { group ->
                                    selectionMode = true
                                    selectedGroupIds = selectedGroupIds + group.groupId
                                },
                                onToggleSelect = { group ->
                                    selectedGroupIds = if (group.groupId in selectedGroupIds) {
                                        selectedGroupIds - group.groupId
                                    } else {
                                        selectedGroupIds + group.groupId
                                    }
                                    if (selectedGroupIds.isEmpty()) {
                                        selectionMode = false
                                    }
                                },
                                onEditCourse = onEditCourse,
                            )
                            if (sectionIndex < sections.lastIndex) {
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

internal data class CourseSemesterSection(
    val key: String,
    val title: String,
    val groups: List<CourseGroupSummary>,
)

internal fun groupCourseGroupsBySemester(
    courseGroups: List<CourseGroupSummary>,
    semesters: List<Semester>,
): List<CourseSemesterSection> {
    if (courseGroups.isEmpty()) return emptyList()
    val semesterById = semesters.associateBy { it.id }
    return courseGroups
        .groupBy { it.semesterId ?: LEGACY_SEMESTER_KEY }
        .map { (semesterKey, groups) ->
            val title = when (semesterKey) {
                LEGACY_SEMESTER_KEY -> "未归属学期"
                else -> semesterById[semesterKey]?.name ?: semesterKey
            }
            CourseSemesterSection(
                key = semesterKey,
                title = title,
                groups = groups.sortedBy { it.name },
            )
        }
        .sortedByDescending { section ->
            when (section.key) {
                LEGACY_SEMESTER_KEY -> Long.MIN_VALUE
                else -> semesterById[section.key]?.startDateEpochDay ?: Long.MIN_VALUE
            }
        }
}

@Composable
private fun SemesterCourseSection(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    groups: List<CourseGroupSummary>,
    selectionMode: Boolean,
    selectedGroupIds: Set<String>,
    onEnterSelection: (CourseGroupSummary) -> Unit,
    onToggleSelect: (CourseGroupSummary) -> Unit,
    onEditCourse: (Long) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = PoolRowHorizontalPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = ScheduleFormStyles.body)
                Text(
                    text = "$count 门课程",
                    style = ScheduleFormStyles.secondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = PoolColors.AccentPrimary,
            )
        }
        if (expanded) {
            groups.forEachIndexed { index, group ->
                CourseGroupRow(
                    group = group,
                    selectionMode = selectionMode,
                    selected = group.groupId in selectedGroupIds,
                    onEdit = { onEditCourse(group.representativeCourseId) },
                    onEnterSelection = { onEnterSelection(group) },
                    onToggleSelect = { onToggleSelect(group) },
                )
                if (index < groups.lastIndex) {
                    PoolDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CourseGroupRow(
    group: CourseGroupSummary,
    selectionMode: Boolean,
    selected: Boolean,
    onEdit: () -> Unit,
    onEnterSelection: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelect() else onEdit()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (selectionMode) {
                        onToggleSelect()
                    } else {
                        onEnterSelection()
                    }
                },
            )
            .padding(horizontal = PoolRowHorizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PoolColors.AccentPrimary,
                    uncheckedColor = PoolColors.TextSecondary,
                    checkmarkColor = PoolColors.Block1,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = group.name, style = ScheduleFormStyles.body)
            Text(
                text = if (group.periodCount > 1) {
                    "${group.periodCount} 个上课时段"
                } else {
                    "1 个上课时段"
                },
                style = ScheduleFormStyles.secondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (!selectionMode) {
            CourseEditActionText(text = "编辑", onClick = onEdit)
        }
    }
}
