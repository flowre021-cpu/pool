package com.example.pool.ui.affair.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.ui.theme.DdlDueSoon
import com.example.pool.ui.theme.DdlOverdue
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.DeadlineUrgency
import com.example.pool.util.ReminderRecurrenceMode
import com.example.pool.util.affairDayLabel
import com.example.pool.util.deadlineColor
import com.example.pool.util.deadlineStatus
import com.example.pool.util.formatEventTimeRange
import com.example.pool.util.formatEventLocation
import com.example.pool.util.formatOpportunityRange
import com.example.pool.util.formatReminderSummary
import com.example.pool.util.isReminderOccurrenceCompleted
import com.example.pool.util.parseReminderRecurrence
import com.example.pool.ui.home.reminderOccurrencesInWindow
import java.time.LocalDate

@Composable
fun AffairListSubtitle(
    affair: AffairEntity,
    modifier: Modifier = Modifier,
) {
    if (affair.type == AffairType.OPPORTUNITY && affair.isDone) return

    when (affair.type) {
        AffairType.TASK -> TaskAffairSubtitle(affair = affair, modifier = modifier)
        AffairType.REMINDER -> ReminderAffairSubtitle(affair = affair, modifier = modifier)
        AffairType.OPPORTUNITY -> OpportunityAffairSubtitle(affair = affair, modifier = modifier)
        AffairType.EVENT -> EventAffairSubtitle(affair = affair, modifier = modifier)
    }
}

@Composable
private fun TaskAffairSubtitle(affair: AffairEntity, modifier: Modifier = Modifier) {
    val status = deadlineStatus(affair.deadline, affair.isDone)
    val color = if (affair.isDone) PoolColors.TextSecondary else deadlineColor(status.urgency)
    val isExpired = !affair.isDone && status.urgency == DeadlineUrgency.OVERDUE

    Row(
        modifier = modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = status.timeText,
            style = TextStyle(fontSize = 12.sp, color = color),
        )
        if (isExpired) {
            ExpiredBadge(color = color)
        } else if (!affair.isDone && status.badgeText != null) {
            Text(
                text = "[${status.badgeText}]",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color),
            )
        }
    }
}

@Composable
private fun ReminderAffairSubtitle(affair: AffairEntity, modifier: Modifier = Modifier) {
    val recurrence = parseReminderRecurrence(affair.recurrenceRule)
    val summary = formatReminderSummary(affair.startAt, recurrence)
    if (affair.startAt == null && recurrence.mode == ReminderRecurrenceMode.NONE && !affair.isDone) {
        Text(
            text = summary,
            modifier = modifier.padding(top = 4.dp),
            style = TextStyle(fontSize = 12.sp, color = PoolColors.TextSecondary),
        )
        return
    }
    val today = LocalDate.now()
    val dayLabel = reminderListDayLabel(affair, today)
    val color = when {
        affair.isDone -> PoolColors.TextSecondary
        dayLabel == "已过期" -> DdlOverdue
        dayLabel in listOf("今天", "明天") -> DdlDueSoon
        else -> PoolColors.TextSecondary
    }

    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = summary,
            style = TextStyle(fontSize = 12.sp, color = color),
        )
        if (dayLabel != null) {
            Text(
                text = "[$dayLabel]",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color),
            )
        }
    }
}

private fun reminderListDayLabel(affair: AffairEntity, today: LocalDate): String? {
    if (affair.isDone) return null
    val recurrence = parseReminderRecurrence(affair.recurrenceRule)
    if (recurrence.mode != ReminderRecurrenceMode.NONE) {
        if (isReminderOccurrenceCompleted(affair, today)) return null
        if (today in reminderOccurrencesInWindow(affair, listOf(today), today)) return "今天"
        val tomorrow = today.plusDays(1)
        if (tomorrow in reminderOccurrencesInWindow(affair, listOf(tomorrow), today)) return "明天"
        return null
    }
    return affairDayLabel(affair.startAt, affair.isDone)
}

@Composable
private fun OpportunityAffairSubtitle(affair: AffairEntity, modifier: Modifier = Modifier) {
    val color = if (affair.isDone) PoolColors.TextSecondary else PoolColors.TextSecondary
    Text(
        text = formatOpportunityRange(affair.startAt, affair.endAt),
        modifier = modifier.padding(top = 4.dp),
        style = TextStyle(fontSize = 12.sp, color = color),
    )
}

@Composable
private fun EventAffairSubtitle(affair: AffairEntity, modifier: Modifier = Modifier) {
    val color = PoolColors.TextSecondary
    Column(modifier = modifier.padding(top = 4.dp)) {
        Text(
            text = formatEventTimeRange(affair.startAt, affair.endAt),
            style = TextStyle(fontSize = 12.sp, color = color),
        )
        Text(
            text = formatEventLocation(affair.location),
            style = TextStyle(fontSize = 12.sp, color = color),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ExpiredBadge(color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(text = "[", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color))
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(11.dp),
        )
        Text(text = " 已过期]", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color))
    }
}
