package com.example.pool.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object SemesterCalendar {
    fun startDate(semester: com.example.pool.data.schedule.Semester): LocalDate =
        LocalDate.ofEpochDay(semester.startDateEpochDay)

    fun endDate(semester: com.example.pool.data.schedule.Semester): LocalDate =
        LocalDate.ofEpochDay(semester.endDateEpochDay)

    fun contains(
        semester: com.example.pool.data.schedule.Semester,
        date: LocalDate,
    ): Boolean = !date.isBefore(startDate(semester)) && !date.isAfter(endDate(semester))

    fun firstMondayOfSemester(semesterStart: LocalDate): LocalDate =
        semesterStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekDates(weekNumber: Int, semesterStart: LocalDate): List<LocalDate> {
        val monday = firstMondayOfSemester(semesterStart).plusWeeks((weekNumber - 1).toLong())
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun weekNumberForDate(today: LocalDate, semesterStart: LocalDate): Int {
        val firstMonday = firstMondayOfSemester(semesterStart)
        val todayMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        if (todayMonday.isBefore(firstMonday)) return 1
        val days = ChronoUnit.DAYS.between(firstMonday, todayMonday)
        return (days / 7).toInt() + 1
    }

    fun weekCount(semester: com.example.pool.data.schedule.Semester): Int {
        return weekCount(startDate(semester), endDate(semester))
    }

    fun weekCount(startDate: LocalDate, endDate: LocalDate): Int {
        val firstMonday = firstMondayOfSemester(startDate)
        val lastMonday = endDate
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (ChronoUnit.WEEKS.between(firstMonday, lastMonday).toInt() + 1)
            .coerceAtLeast(1)
    }

    fun formatWeekTitle(weekNumber: Int, semesterStart: LocalDate, semesterName: String): String {
        val dates = weekDates(weekNumber, semesterStart)
        val start = dates.first()
        val end = dates.last()
        val range = "${start.monthValue}月${start.dayOfMonth}日 - ${end.monthValue}月${end.dayOfMonth}日"
        return "$semesterName · 第 $weekNumber 周  $range"
    }

    fun normalizedSemesterStart(startDate: LocalDate): LocalDate =
        firstMondayOfSemester(startDate)

    fun endDateForWeekCount(startDate: LocalDate, weekCount: Int): LocalDate {
        val firstMonday = firstMondayOfSemester(startDate)
        return firstMonday.plusWeeks(weekCount.coerceIn(1, 26).toLong()).minusDays(1)
    }
}
