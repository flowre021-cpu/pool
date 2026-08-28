package com.example.pool.ui.affair

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import com.example.pool.ui.affair.components.AffairListSubtitle
import com.example.pool.ui.affair.listEmptyHint
import com.example.pool.ui.affair.showsDoneCheckbox
import com.example.pool.ui.components.PoolSelectionTopBarActions
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.isAffairCheckboxChecked

private val AffairCheckboxWidth = 48.dp
private val AffairContentTopPadding = 14.dp
private val AffairCollapsedTopPadding = 10.dp
private val AffairListFabBottomPadding = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AffairListScreen(
    affairType: AffairType,
    repository: PlannerRepository,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appContext = LocalContext.current.applicationContext
    val viewModel: AffairViewModel = viewModel(
        key = affairType.name,
        factory = AffairViewModelFactory(repository, appContext, affairType),
    )
    val affairs by viewModel.affairs.collectAsStateWithLifecycle()
    val sections = remember(affairs) { partitionAffairsForList(affairs) }
    var foldedExpanded by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedAffairIds by remember { mutableStateOf(setOf<Long>()) }
    var pendingDelete by remember { mutableStateOf<List<AffairEntity>?>(null) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedAffairIds = emptySet()
    }

    fun toggleSelection(affair: AffairEntity) {
        selectedAffairIds = if (affair.id in selectedAffairIds) {
            selectedAffairIds - affair.id
        } else {
            selectedAffairIds + affair.id
        }
        if (selectedAffairIds.isEmpty()) {
            exitSelectionMode()
        }
    }

    pendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = PoolColors.Block1,
            titleContentColor = PoolColors.TextPrimary,
            textContentColor = PoolColors.TextPrimary,
            title = { Text("确认删除") },
            text = { Text("确定删除选中的 ${toDelete.size} 项吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAffairs(toDelete)
                        exitSelectionMode()
                        pendingDelete = null
                    },
                ) {
                    Text("删除", color = PoolColors.DeleteRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消", color = PoolColors.TextPrimary)
                }
            },
            tonalElevation = AlertDialogDefaults.TonalElevation,
        )
    }

    if (affairs.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = affairType.listEmptyHint(),
                color = PoolColors.TextSecondary,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = AffairListFabBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selectionMode) {
                item(key = "selection_bar") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = ::exitSelectionMode) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "取消选择",
                                tint = PoolColors.AccentPrimary,
                            )
                        }
                        Text(
                            text = "已选择 ${selectedAffairIds.size} 项",
                            modifier = Modifier.weight(1f),
                            color = PoolColors.TextPrimary,
                        )
                        PoolSelectionTopBarActions(
                            selectedCount = selectedAffairIds.size,
                            onDelete = {
                                val selected = affairs.filter { it.id in selectedAffairIds }
                                if (selected.isNotEmpty()) pendingDelete = selected
                            },
                        )
                    }
                }
            }
            items(sections.active, key = { it.id }) { affair ->
                AffairListCard(
                    affair = affair,
                    selectionMode = selectionMode,
                    selected = affair.id in selectedAffairIds,
                    onToggleDone = { if (!selectionMode) viewModel.toggleDone(affair) },
                    onClick = {
                        if (selectionMode) toggleSelection(affair) else onEdit(affair.id)
                    },
                    onLongClick = {
                        if (selectionMode) {
                            toggleSelection(affair)
                        } else {
                            selectionMode = true
                            selectedAffairIds = selectedAffairIds + affair.id
                        }
                    },
                )
            }
            if (sections.folded.isNotEmpty()) {
                item(key = "folded_header") {
                    AffairFoldSectionHeader(
                        title = affairType.foldedSectionTitle(),
                        expiredCount = sections.expiredCount,
                        showExpiredCountInRed = affairType.showExpiredCountInRed(),
                        totalCount = sections.folded.size,
                        expanded = foldedExpanded,
                        onToggle = { foldedExpanded = !foldedExpanded },
                    )
                }
                if (foldedExpanded) {
                    items(sections.folded, key = { "folded-${it.id}" }) { affair ->
                        AffairListCard(
                            affair = affair,
                            selectionMode = selectionMode,
                            selected = affair.id in selectedAffairIds,
                            onToggleDone = { if (!selectionMode) viewModel.toggleDone(affair) },
                            onClick = {
                                if (selectionMode) toggleSelection(affair) else onEdit(affair.id)
                            },
                            onLongClick = {
                                if (selectionMode) {
                                    toggleSelection(affair)
                                } else {
                                    selectionMode = true
                                    selectedAffairIds = selectedAffairIds + affair.id
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AffairFoldSectionHeader(
    title: String,
    expiredCount: Int,
    showExpiredCountInRed: Boolean,
    totalCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = TextStyle(fontSize = 13.sp, color = PoolColors.TextSecondary),
            )
            if (expiredCount > 0) {
                Text(
                    text = " · $expiredCount 已过期",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = if (showExpiredCountInRed) PoolColors.DeleteRed else PoolColors.TextSecondary,
                    ),
                )
            }
        }
        Text(
            text = "$totalCount",
            style = TextStyle(fontSize = 13.sp, color = PoolColors.TextSecondary),
            modifier = Modifier.padding(end = 4.dp),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = PoolColors.TextSecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AffairListCard(
    affair: AffairEntity,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isOpportunityCollapsed = affair.type == AffairType.OPPORTUNITY && affair.isDone
    val checkboxChecked = isAffairCheckboxChecked(affair)
    val showDoneCheckbox = affair.type.showsDoneCheckbox()
    val verticalPadding = if (isOpportunityCollapsed) 6.dp else 10.dp
    val contentTopPadding = if (isOpportunityCollapsed) AffairCollapsedTopPadding else AffairContentTopPadding

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
        color = if (isOpportunityCollapsed) PoolColors.Block else PoolColors.Block1,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = verticalPadding, bottom = verticalPadding),
            verticalAlignment = Alignment.Top,
        ) {
            if (selectionMode || showDoneCheckbox) {
                Box(
                    modifier = Modifier
                        .width(AffairCheckboxWidth)
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
                    } else if (showDoneCheckbox) {
                        Checkbox(
                            checked = checkboxChecked,
                            onCheckedChange = { onToggleDone() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PoolColors.AccentPrimary,
                                uncheckedColor = PoolColors.TextSecondary,
                                checkmarkColor = PoolColors.Block1,
                            ),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        top = if (selectionMode || showDoneCheckbox) contentTopPadding else AffairContentTopPadding,
                        start = if (selectionMode || showDoneCheckbox) 0.dp else 12.dp,
                    ),
            ) {
                Text(
                    text = affair.title,
                    style = TextStyle(
                        fontSize = if (isOpportunityCollapsed) 14.sp else 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isOpportunityCollapsed -> PoolColors.TextSecondary
                            affair.isDone && showDoneCheckbox -> PoolColors.TextSecondary
                            checkboxChecked && showDoneCheckbox -> PoolColors.TextSecondary
                            else -> PoolColors.TextPrimary
                        },
                    ),
                    textDecoration = if (affair.type == AffairType.TASK && affair.isDone) {
                        TextDecoration.LineThrough
                    } else {
                        null
                    },
                )
                if (!isOpportunityCollapsed) {
                    AffairListSubtitle(affair = affair)
                }
            }
        }
    }
}
