package com.example.pool.util

const val DEFAULT_WEEK_COUNT = 20

fun defaultSelectedWeeks(count: Int = DEFAULT_WEEK_COUNT): List<Int> = (1..count).toList()

fun toggleWeekSelection(selectedWeeks: List<Int>, week: Int, total: Int = DEFAULT_WEEK_COUNT): List<Int> {
    if (week !in 1..total) return selectedWeeks
    return if (week in selectedWeeks) {
        selectedWeeks.filter { it != week }.sorted()
    } else {
        (selectedWeeks + week).sorted()
    }
}

/**
 * When semester length grows, append newly available weeks to [selectedWeeks].
 * When it shrinks, drop weeks outside `1..newTotal`.
 */
fun syncSelectedWeeksForSemesterLength(
    selectedWeeks: List<Int>,
    previousTotal: Int,
    newTotal: Int,
): List<Int> {
    val safeNew = newTotal.coerceAtLeast(1)
    val trimmed = selectedWeeks.filter { it in 1..safeNew }
    if (safeNew <= previousTotal) return trimmed.sorted()
    val additions = ((previousTotal.coerceAtLeast(0) + 1)..safeNew).toList()
    return (trimmed + additions).distinct().sorted()
}

fun isContiguousFullWeekSelection(selectedWeeks: List<Int>): Boolean {
    val max = selectedWeeks.maxOrNull() ?: return false
    if (max <= 0) return false
    return selectedWeeks.size == max && selectedWeeks.toSet() == (1..max).toSet()
}

fun formatSelectedWeeksSummary(
    weeks: List<Int>,
    totalWeeks: Int = DEFAULT_WEEK_COUNT,
): String {
    if (weeks.isEmpty()) return "未选择上课周"
    val total = totalWeeks.coerceAtLeast(1)
    if (weeks.size == total && weeks.toSet() == (1..total).toSet()) {
        return "第 1-$total 周"
    }
    val exempt = (1..total).filter { it !in weeks }
    return if (exempt.size <= 3) {
        "免修：第 ${exempt.joinToString("、") { "$it" }} 周"
    } else {
        "共 ${weeks.size} 周上课"
    }
}
