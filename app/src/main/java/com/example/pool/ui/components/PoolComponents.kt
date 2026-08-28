package com.example.pool.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.formatDatePickerHeadline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = PoolColors.AccentPrimary,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PoolColors.NavBar,
            titleContentColor = PoolColors.TextPrimary,
            navigationIconContentColor = PoolColors.AccentPrimary,
        ),
    )
}

/** 横线铺满区块宽度；正文左右 24dp 内边距，线比文字左右各长出 24dp */
@Composable
fun PoolDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = Color.LightGray.copy(alpha = 0.3f),
        thickness = 1.dp,
    )
}

@Composable
fun PoolBlock1(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(PoolColors.Block1),
    ) {
        content()
    }
}

/** 列表/入口行统一内边距，配合 PoolDivider 实现横线比文字各长 24dp */
val PoolRowHorizontalPadding = 24.dp

@Composable
fun PoolNavRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PoolRowHorizontalPadding, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = PoolColors.AccentPrimary,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = PoolColors.TextPrimary,
        )
    }
}

@Composable
fun PoolPreferenceSectionHeader(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PoolRowHorizontalPadding, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = PoolColors.AccentPrimary,
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = PoolColors.TextSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun PoolPreferenceOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    trailingColor: Color? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = PoolColors.AccentPrimary,
            ),
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = PoolColors.TextPrimary,
        )
        if (trailingColor != null) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(trailingColor),
            )
        }
    }
}

@Composable
fun PoolAppIconOptionRow(
    label: String,
    selected: Boolean,
    previewDrawableRes: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                selectedColor = PoolColors.AccentPrimary,
            ),
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = PoolColors.TextPrimary,
        )
        Image(
            painter = painterResource(previewDrawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
    }
}

@Composable
fun PoolAddFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = PoolColors.FabMint,
        contentColor = PoolColors.AccentPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun PoolConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "删除",
    confirmColor: Color = PoolColors.DeleteRed,
    dismissText: String = "取消",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PoolColors.Block1,
        titleContentColor = PoolColors.TextPrimary,
        textContentColor = PoolColors.TextPrimary,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = PoolColors.TextPrimary)
            }
        },
        tonalElevation = AlertDialogDefaults.TonalElevation,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoolDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    datePickerState: DatePickerState,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = PoolColors.Block1,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PoolColors.Block1),
            ) {
                DatePicker(
                    modifier = Modifier.fillMaxWidth(),
                    state = datePickerState,
                    colors = PoolPickerDefaults.datePickerColors(),
                    showModeToggle = false,
                    title = { },
                    headline = {
                        Text(
                            text = formatDatePickerHeadline(datePickerState.selectedDateMillis),
                            style = MaterialTheme.typography.headlineLarge,
                            color = PoolColors.Accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                        )
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = PoolColors.TextPrimary)
                    }
                    TextButton(onClick = onConfirm) {
                        Text("确定", color = PoolColors.AccentPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
object PoolPickerDefaults {
    /** @param accent 选择过程中的强调色（较亮）；完成确认请单独使用 [PoolColors.AccentPrimary] */
    @Composable
    fun datePickerColors(accent: Color = PoolColors.Accent) = DatePickerDefaults.colors(
        containerColor = PoolColors.Block1,
        titleContentColor = PoolColors.TextPrimary,
        headlineContentColor = accent,
        weekdayContentColor = PoolColors.TextSecondary,
        subheadContentColor = PoolColors.TextSecondary,
        yearContentColor = PoolColors.TextPrimary,
        currentYearContentColor = accent,
        selectedYearContainerColor = accent.copy(alpha = 0.35f),
        selectedYearContentColor = PoolColors.AccentPrimary,
        dayContentColor = PoolColors.TextPrimary,
        selectedDayContainerColor = accent.copy(alpha = 0.35f),
        selectedDayContentColor = PoolColors.AccentPrimary,
        todayContentColor = accent,
        todayDateBorderColor = accent,
    )

    /** @param accent 选择过程中的强调色（较亮） */
    @Composable
    fun timePickerColors(accent: Color = PoolColors.Accent) = TimePickerDefaults.colors(
        containerColor = PoolColors.Block1,
        clockDialColor = PoolColors.Block,
        clockDialSelectedContentColor = Color.White,
        clockDialUnselectedContentColor = PoolColors.TextPrimary,
        selectorColor = accent,
        periodSelectorBorderColor = PoolColors.Divider,
        periodSelectorSelectedContainerColor = accent,
        periodSelectorUnselectedContainerColor = PoolColors.Block,
        periodSelectorSelectedContentColor = Color.White,
        periodSelectorUnselectedContentColor = PoolColors.TextPrimary,
        timeSelectorSelectedContainerColor = accent,
        timeSelectorUnselectedContainerColor = PoolColors.Block,
        timeSelectorSelectedContentColor = Color.White,
        timeSelectorUnselectedContentColor = PoolColors.TextPrimary,
    )
}
