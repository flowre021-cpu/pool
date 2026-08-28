package com.example.pool.widget

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal fun formatMinutesRange(startMinutes: Int, endMinutes: Int): String {
    fun fmt(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)
    return "${fmt(startMinutes)}-${fmt(endMinutes)}"
}

internal fun formatWidgetDeadlineRemaining(deadlineMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    val ddl = Instant.ofEpochMilli(deadlineMillis).atZone(zone)
    if (!ddl.isAfter(now)) return "过期"
    val minutes = Duration.between(now, ddl).toMinutes()
    if (minutes < 60) return "${minutes.coerceAtLeast(1)}m"
    val hours = Duration.between(now, ddl).toHours()
    if (hours < 24) return "${hours}h"
    val days = Duration.between(
        now.toLocalDate().atStartOfDay(zone),
        ddl.toLocalDate().atStartOfDay(zone),
    ).toDays()
    return "${days.coerceAtLeast(1)}日"
}

internal fun widgetLocationLabel(location: String?): String? =
    location?.trim()?.takeIf { it.isNotEmpty() }
