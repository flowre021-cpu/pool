package com.example.pool.util

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import java.time.LocalDate

fun hasRecurringReminder(affair: AffairEntity): Boolean =
    affair.type == AffairType.REMINDER &&
        parseReminderRecurrence(affair.recurrenceRule).mode != ReminderRecurrenceMode.NONE

fun parseCompletedOccurrenceDays(raw: String?): Set<Long> =
    raw?.split(',')
        ?.mapNotNull { it.trim().toLongOrNull() }
        ?.toSet()
        ?: emptySet()

fun encodeCompletedOccurrenceDays(days: Set<Long>): String? =
    days.sorted().joinToString(",").takeIf { it.isNotEmpty() }

fun isReminderOccurrenceCompleted(reminder: AffairEntity, date: LocalDate): Boolean {
    if (reminder.isDone) return true
    return date.toEpochDay() in parseCompletedOccurrenceDays(reminder.completedOccurrenceDays)
}

fun isAffairCheckboxChecked(
    affair: AffairEntity,
    today: LocalDate = LocalDate.now(),
): Boolean = when {
    hasRecurringReminder(affair) -> isReminderOccurrenceCompleted(affair, today)
    else -> affair.isDone
}

fun toggleAffairDone(
    affair: AffairEntity,
    occurrenceDate: LocalDate = LocalDate.now(),
): AffairEntity {
    if (hasRecurringReminder(affair)) {
        val epochDay = occurrenceDate.toEpochDay()
        val completed = parseCompletedOccurrenceDays(affair.completedOccurrenceDays).toMutableSet()
        if (epochDay in completed) {
            completed.remove(epochDay)
        } else {
            completed.add(epochDay)
        }
        return affair.copy(completedOccurrenceDays = encodeCompletedOccurrenceDays(completed))
    }
    return affair.copy(isDone = !affair.isDone)
}
