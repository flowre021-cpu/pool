package com.example.pool.util

enum class ReminderRecurrenceMode {
    NONE,
    DAILY,
    WEEKLY,
}

data class ReminderRecurrence(
    val mode: ReminderRecurrenceMode = ReminderRecurrenceMode.NONE,
    /** 1=周一 … 7=周日；仅 WEEKLY 有效，可多选 */
    val weeklyDays: Set<Int> = emptySet(),
) {
    fun encode(): String? = when (mode) {
        ReminderRecurrenceMode.NONE -> null
        ReminderRecurrenceMode.DAILY -> RULE_DAILY
        ReminderRecurrenceMode.WEEKLY -> {
            val days = weeklyDays.sorted().joinToString(",")
            if (days.isEmpty()) null else "$RULE_WEEKLY_PREFIX$days"
        }
    }

    fun isValid(): Boolean = when (mode) {
        ReminderRecurrenceMode.NONE, ReminderRecurrenceMode.DAILY -> true
        ReminderRecurrenceMode.WEEKLY -> weeklyDays.isNotEmpty()
    }
}

private const val RULE_DAILY = "DAILY"
private const val RULE_WEEKLY_PREFIX = "WEEKLY:"

fun parseReminderRecurrence(rule: String?): ReminderRecurrence {
    val raw = rule?.trim().orEmpty()
    if (raw.isEmpty()) return ReminderRecurrence()
    when {
        raw.equals(RULE_DAILY, ignoreCase = true) ||
            raw.equals("FREQ=DAILY", ignoreCase = true) -> {
            return ReminderRecurrence(mode = ReminderRecurrenceMode.DAILY)
        }
        raw.startsWith(RULE_WEEKLY_PREFIX, ignoreCase = true) -> {
            val days = raw
                .substringAfter(':')
                .split(',')
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .toSet()
            return ReminderRecurrence(
                mode = ReminderRecurrenceMode.WEEKLY,
                weeklyDays = days,
            )
        }
    }
    return ReminderRecurrence()
}

fun formatReminderRecurrence(recurrence: ReminderRecurrence): String? = when (recurrence.mode) {
    ReminderRecurrenceMode.NONE -> null
    ReminderRecurrenceMode.DAILY -> "每天"
    ReminderRecurrenceMode.WEEKLY -> {
        if (recurrence.weeklyDays.isEmpty()) {
            "每周"
        } else {
            val labels = recurrence.weeklyDays.sorted().joinToString("、") { dayOfWeekLabel(it) }
            "每周$labels"
        }
    }
}

fun formatReminderSummary(
    startAt: Long?,
    recurrence: ReminderRecurrence,
): String {
    val recurLabel = formatReminderRecurrence(recurrence)
    val scheduleLabel = formatReminderSchedule(startAt)
    return when {
        recurLabel != null && scheduleLabel != "随时提示" -> "$recurLabel · $scheduleLabel"
        recurLabel != null -> recurLabel
        else -> scheduleLabel
    }
}

fun reminderRecurrenceValidationError(recurrence: ReminderRecurrence): String? =
    if (recurrence.mode == ReminderRecurrenceMode.WEEKLY && recurrence.weeklyDays.isEmpty()) {
        "请选择重复的星期"
    } else {
        null
    }
