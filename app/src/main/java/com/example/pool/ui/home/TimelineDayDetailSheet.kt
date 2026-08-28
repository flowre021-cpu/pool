package com.example.pool.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.ui.affair.affairSortMillis
import com.example.pool.ui.affair.components.AffairListSubtitle
import com.example.pool.ui.affair.hubTitle
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.formatDateHeader

@Composable
fun TimelineDayDetailDialog(
    detail: TimelineDayDetail,
    onDismiss: () -> Unit,
    onEditTask: (Long) -> Unit,
    onEditOpportunity: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortedTasks = detail.tasks.sortedWith(compareBy({ affairSortMillis(it) }, { it.id }))
    val sortedOpportunities = detail.opportunities.sortedWith(compareBy({ affairSortMillis(it) }, { it.id }))
    val sortedReminders = detail.reminders.sortedWith(compareBy({ affairSortMillis(it) }, { it.id }))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 480.dp),
            shape = RoundedCornerShape(16.dp),
            color = PoolColors.Block1,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(
                    text = formatDateHeader(detail.date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PoolColors.DateHeader,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                if (sortedTasks.isEmpty() && sortedOpportunities.isEmpty() && sortedReminders.isEmpty()) {
                    Text(
                        text = "当日暂无待办、周期事项或提醒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PoolColors.TextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    if (sortedTasks.isNotEmpty()) {
                        TimelineDetailSection(title = AffairType.TASK.hubTitle()) {
                            sortedTasks.forEach { affair ->
                                TimelineDetailItem(
                                    affair = affair,
                                    onClick = { onEditTask(affair.id) },
                                )
                            }
                        }
                    }
                    if (sortedOpportunities.isNotEmpty()) {
                        TimelineDetailSection(title = AffairType.OPPORTUNITY.hubTitle()) {
                            sortedOpportunities.forEach { affair ->
                                TimelineDetailItem(
                                    affair = affair,
                                    onClick = { onEditOpportunity(affair.id) },
                                )
                            }
                        }
                    }
                    if (sortedReminders.isNotEmpty()) {
                        TimelineDetailSection(title = AffairType.REMINDER.hubTitle()) {
                            sortedReminders.forEach { affair ->
                                TimelineDetailItem(
                                    affair = affair,
                                    onClick = { onEditReminder(affair.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineDetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = PoolColors.Accent,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        content()
        HorizontalDivider(
            color = PoolColors.Divider.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun TimelineDetailItem(
    affair: AffairEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = affair.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (affair.isDone) PoolColors.TextSecondary else PoolColors.TextPrimary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        AffairListSubtitle(affair = affair)
    }
}
