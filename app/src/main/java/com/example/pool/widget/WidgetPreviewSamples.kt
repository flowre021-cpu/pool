package com.example.pool.widget

/** 小部件选择器预览用的静态示例数据（非真实用户数据）。 */
internal object WidgetPreviewSamples {
    val todayItems = listOf(
        WidgetScheduleItem(
            id = 1L,
            kind = WidgetScheduleKind.COURSE,
            title = "数学课",
            location = "A101",
            startMinutes = 14 * 60,
            endMinutes = 15 * 60 + 35,
            colorArgb = 0xFF98B8E8L,
        ),
        WidgetScheduleItem(
            id = 2L,
            kind = WidgetScheduleKind.EVENT,
            title = "组会",
            location = "线上",
            startMinutes = 17 * 60,
            endMinutes = 17 * 60 + 55,
            colorArgb = 0xFF88D4BCL,
        ),
    )

    val deadlineItems = listOf(
        WidgetDeadlineItem(id = 1L, title = "报告", remainingLabel = "2h", deadlineMillis = 1L),
        WidgetDeadlineItem(id = 2L, title = "论文", remainingLabel = "3日", deadlineMillis = 2L),
    )

    val reminderItems = listOf(
        WidgetReminderItem(id = 1L, title = "拿快递", startMinutes = 9 * 60),
        WidgetReminderItem(id = 2L, title = "洗衣服", startMinutes = 18 * 60),
    )
}
