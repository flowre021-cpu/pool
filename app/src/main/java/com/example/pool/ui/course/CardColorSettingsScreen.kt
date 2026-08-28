package com.example.pool.ui.course

import android.widget.Toast
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDivider
import com.example.pool.ui.components.PoolPreferenceOptionRow
import com.example.pool.ui.components.PoolPreferenceSectionHeader
import com.example.pool.ui.components.PoolRowHorizontalPadding
import com.example.pool.ui.course.components.CourseColorPickerDialog
import com.example.pool.ui.course.components.CourseEditSectionDivider
import com.example.pool.ui.course.model.CourseColorPalettes
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.ui.course.model.colorFromArgbLong
import com.example.pool.ui.theme.PoolColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CardColorSettingsScreen(
    viewModel: CourseViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val colorPrefs = remember { CardColorPreferences(context) }
    val courseGroups by viewModel.courseGroups.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()

    var useCustomPalette by remember { mutableStateOf(colorPrefs.usesCustomPalette()) }
    var customPalette by remember { mutableStateOf(colorPrefs.getCustomPaletteColors()) }
    var editingCustomSlot by remember { mutableIntStateOf(-1) }
    var editingGroupId by remember { mutableStateOf<String?>(null) }

    val activeColors = remember(useCustomPalette, customPalette) {
        if (useCustomPalette && customPalette.isNotEmpty()) customPalette
        else CourseColorPalettes.defaultColors
    }

    val groupColors = remember(courses) {
        courses.groupBy { it.groupId }.mapValues { (_, rows) -> rows.first().cardColor }
    }

    fun persistMode() {
        colorPrefs.setUsesCustomPalette(useCustomPalette)
        colorPrefs.setCustomPaletteColors(customPalette)
    }

    editingCustomSlot.takeIf { it >= 0 }?.let { slot ->
        CourseColorPickerDialog(
            selected = customPalette.getOrElse(slot) { SchedulePastelColors.default },
            onDismiss = { editingCustomSlot = -1 },
            onColorSelected = { picked ->
                val next = customPalette.toMutableList()
                while (next.size <= slot) next.add(picked)
                next[slot] = picked
                customPalette = next.take(CourseColorPalettes.MAX_CUSTOM_COLORS)
                useCustomPalette = true
                persistMode()
                editingCustomSlot = -1
            },
        )
    }

    editingGroupId?.let { groupId ->
        val current = groupColors[groupId]?.let(::colorFromArgbLong) ?: SchedulePastelColors.default
        CourseColorPickerDialog(
            selected = current,
            onDismiss = { editingGroupId = null },
            onColorSelected = { picked ->
                viewModel.updateGroupCardColor(groupId, picked)
                editingGroupId = null
            },
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("课程颜色") },
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
            PoolBlock1(modifier = Modifier.padding(top = 12.dp)) {
                Column {
                    PoolPreferenceSectionHeader(
                        title = "使用色卡",
                        hint = "课程按顺序与色卡颜色一一对应",
                    )
                    PoolPreferenceOptionRow(
                        label = "默认色卡",
                        selected = !useCustomPalette,
                        onSelect = {
                            useCustomPalette = false
                            persistMode()
                        },
                    )
                    PoolDivider()
                    PoolPreferenceOptionRow(
                        label = "自定义色卡",
                        selected = useCustomPalette,
                        onSelect = {
                            if (customPalette.isEmpty()) {
                                Toast.makeText(context, "请先在选色微调中保存颜色，或点击下方圆点编辑", Toast.LENGTH_SHORT).show()
                            }
                            useCustomPalette = true
                            persistMode()
                        },
                    )

                    PoolDivider()

                    PoolPreferenceSectionHeader(
                        title = if (useCustomPalette) "自定义色卡" else "默认色卡",
                        hint = if (useCustomPalette) {
                            "点击编辑 · 长按删除"
                        } else {
                            "${CourseColorPalettes.defaultColors.size} 色"
                        },
                    )
                    PaletteSwatchFlow(
                        colors = if (useCustomPalette) customPalette else activeColors,
                        editable = useCustomPalette,
                        onEditColor = { index -> editingCustomSlot = index },
                        onAddColor = {
                            editingCustomSlot = customPalette.size.coerceAtMost(
                                CourseColorPalettes.MAX_CUSTOM_COLORS - 1,
                            )
                        },
                        onDeleteColor = { color ->
                            customPalette = colorPrefs.removeCustomColor(color)
                            if (customPalette.isEmpty()) {
                                useCustomPalette = false
                                colorPrefs.setUsesCustomPalette(false)
                                Toast.makeText(context, "已删除全部自定义色，已切回默认色卡", Toast.LENGTH_SHORT).show()
                            } else {
                                persistMode()
                                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                            }
                        },
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PoolRowHorizontalPadding, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                if (useCustomPalette && customPalette.isEmpty()) {
                                    Toast.makeText(context, "自定义色卡为空", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                viewModel.reassignAllCourseColors(activeColors) {
                                    Toast.makeText(context, "已按当前色卡重新分配", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Text("按当前色卡重新分配", color = PoolColors.AccentPrimary)
                        }
                    }
                }
            }

            PoolBlock1(modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)) {
                Column {
                    PoolPreferenceSectionHeader(
                        title = "课程颜色",
                        hint = "点击可单独修改",
                    )
                    if (courseGroups.isEmpty()) {
                        Text(
                            text = "暂无课程",
                            color = PoolColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = PoolRowHorizontalPadding, vertical = 16.dp),
                        )
                    } else {
                        courseGroups.forEachIndexed { index, group ->
                            val color = groupColors[group.groupId]?.let(::colorFromArgbLong)
                                ?: SchedulePastelColors.default
                            CourseColorRow(
                                name = group.name,
                                color = color,
                                onEdit = { editingGroupId = group.groupId },
                            )
                            if (index < courseGroups.lastIndex) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteSwatchFlow(
    colors: List<Color>,
    editable: Boolean,
    onEditColor: (Int) -> Unit,
    onAddColor: () -> Unit,
    onDeleteColor: (Color) -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PoolRowHorizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (editable) {
            colors.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color, CircleShape)
                        .border(1.dp, PoolColors.Divider, CircleShape)
                        .combinedClickable(
                            onClick = { onEditColor(index) },
                            onLongClick = { onDeleteColor(color) },
                        ),
                )
            }
            if (colors.size < CourseColorPalettes.MAX_CUSTOM_COLORS) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PoolColors.BlockDeep, CircleShape)
                        .border(1.dp, PoolColors.Divider, CircleShape)
                        .clickable(onClick = onAddColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加颜色",
                        tint = PoolColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color, CircleShape)
                        .border(1.dp, PoolColors.Divider, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun CourseColorRow(
    name: String,
    color: Color,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = PoolRowHorizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
        )
        Text(
            text = name,
            color = PoolColors.TextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Text(
            text = "编辑",
            color = PoolColors.AccentPrimary,
        )
    }
}
