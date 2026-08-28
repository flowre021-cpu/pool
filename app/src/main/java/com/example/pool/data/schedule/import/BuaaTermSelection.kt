package com.example.pool.data.schedule.import

import java.time.LocalDate

enum class BuaaTermSeason(val label: String) {
    SPRING("春季"),
    SUMMER("夏季"),
    AUTUMN("秋季"),
}

/** 北航课表 API 请求模式：春/夏按周拉取，秋季一次拉全学期。 */
enum class BuaaScheduleFetchMode(val typeParam: String) {
    BY_WEEK("week"),
    BY_CLASS("class"),
}

/**
 * 北航教务 termCode 与用户可选「年份 + 春/夏/秋」之间的转换。
 *
 * 正方常见编码：`-1` 秋季、`-2` 春季、`-3` 夏季（小学期）。
 * 例：2026 春季 → `2025-2026-2`，2026 夏季 → `2025-2026-3`，2026 秋季 → `2026-2027-1`。
 */
object BuaaTermSelection {
    fun toTermCode(calendarYear: Int, season: BuaaTermSeason): String = when (season) {
        BuaaTermSeason.SPRING -> "${calendarYear - 1}-$calendarYear-2"
        BuaaTermSeason.SUMMER -> "${calendarYear - 1}-$calendarYear-3"
        BuaaTermSeason.AUTUMN -> "$calendarYear-${calendarYear + 1}-1"
    }

    fun displayLabel(calendarYear: Int, season: BuaaTermSeason): String =
        "${calendarYear} 年${season.label}学期"

    fun toSemesterId(calendarYear: Int, season: BuaaTermSeason): String = when (season) {
        BuaaTermSeason.SPRING -> "$calendarYear-spring"
        BuaaTermSeason.SUMMER -> "$calendarYear-summer"
        BuaaTermSeason.AUTUMN -> "$calendarYear-fall"
    }

    fun fetchMode(season: BuaaTermSeason): BuaaScheduleFetchMode = when (season) {
        BuaaTermSeason.SPRING, BuaaTermSeason.SUMMER -> BuaaScheduleFetchMode.BY_WEEK
        BuaaTermSeason.AUTUMN -> BuaaScheduleFetchMode.BY_CLASS
    }

    fun defaultSelection(date: LocalDate = LocalDate.now()): Pair<Int, BuaaTermSeason> = when (date.monthValue) {
        in 8..12 -> date.year to BuaaTermSeason.AUTUMN
        in 6..7 -> date.year to BuaaTermSeason.SUMMER
        in 2..5 -> date.year to BuaaTermSeason.SPRING
        else -> date.year to BuaaTermSeason.AUTUMN
    }

    /** 近 3 个自然年（当前年 −2 至当前年），最新在前。 */
    fun yearOptions(anchorYear: Int = LocalDate.now().year): List<Int> =
        ((anchorYear - 2)..anchorYear).reversed().toList()
}
