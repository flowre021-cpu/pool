package com.example.pool.widget

import com.example.pool.data.AffairType

enum class WidgetScheduleKind {
    COURSE,
    EVENT,
}

data class WidgetScheduleItem(
    val id: Long,
    val kind: WidgetScheduleKind,
    val title: String,
    val location: String?,
    val startMinutes: Int,
    val endMinutes: Int,
    val colorArgb: Long,
)

data class WidgetDeadlineItem(
    val id: Long,
    val title: String,
    val remainingLabel: String,
    val deadlineMillis: Long,
)

data class WidgetReminderItem(
    val id: Long,
    val title: String,
    /** 当天分钟数；无具体时刻时为 0 */
    val startMinutes: Int = 0,
)

data class WidgetSnapshot(
    val todayItems: List<WidgetScheduleItem> = emptyList(),
    val deadlineItems: List<WidgetDeadlineItem> = emptyList(),
    val reminderItems: List<WidgetReminderItem> = emptyList(),
)

sealed class WidgetTapTarget {
    data object Home : WidgetTapTarget()

    data object Courses : WidgetTapTarget()

    data class AffairList(val type: AffairType) : WidgetTapTarget()
}
