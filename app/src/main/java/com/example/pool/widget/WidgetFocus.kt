package com.example.pool.widget

import java.time.LocalTime

/**
 * Glance 无法 programmatic scroll。刷新时把「当前/下一项」挪到列表开头，后续仍按时序排列；
 * 若当天已全部过去，则保持完整时序（用户可滚到底查看最后一项）。
 */
internal object WidgetFocus {
    internal fun reorderTodayItemsForFocus(
        items: List<WidgetScheduleItem>,
        now: LocalTime = LocalTime.now(),
    ): List<WidgetScheduleItem> {
        if (items.size <= 1) return items
        val nowMinutes = now.hour * 60 + now.minute
        if (items.all { it.endMinutes <= nowMinutes }) return items
        val focusIdx = items.indexOfFirst { item ->
            item.startMinutes <= nowMinutes && item.endMinutes > nowMinutes
        }.takeIf { it >= 0 }
            ?: items.indexOfFirst { it.startMinutes > nowMinutes }.takeIf { it >= 0 }
            ?: return items
        return reorderFromFocusForward(items, focusIdx)
    }

    /** DDL 已按 deadline 升序；最近一项自然在首位，无需旋转。 */
    internal fun reorderDeadlineItemsForFocus(
        items: List<WidgetDeadlineItem>,
    ): List<WidgetDeadlineItem> = items

    internal fun reorderReminderItemsForFocus(
        items: List<WidgetReminderItem>,
        now: LocalTime = LocalTime.now(),
    ): List<WidgetReminderItem> {
        if (items.size <= 1) return items
        val nowMinutes = now.hour * 60 + now.minute
        val hasUpcoming = items.any { it.startMinutes > nowMinutes }
        if (!hasUpcoming) return items
        val focusIdx = items.indexOfFirst { it.startMinutes > nowMinutes }
        return reorderFromFocusForward(items, focusIdx)
    }

    private fun <T> reorderFromFocusForward(items: List<T>, focusIndex: Int): List<T> {
        if (focusIndex <= 0) return items
        return items.drop(focusIndex) + items.take(focusIndex)
    }
}
