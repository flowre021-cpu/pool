package com.example.pool.ui.course.model

import com.example.pool.data.schedule.CourseFormData
import com.example.pool.data.schedule.CourseTimePeriodData
import com.example.pool.data.schedule.ScheduleCourse
import com.example.pool.data.schedule.ScheduleTimeSlot
import com.example.pool.util.defaultSelectedWeeks
import java.util.UUID

/** UI 层课程卡片数据 */
data class Course(
    val id: Long,
    val groupId: String,
    val name: String,
    val classroom: String?,
    val teacher: String?,
    val note: String?,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val cardColor: androidx.compose.ui.graphics.Color,
    val selectedWeeks: List<Int>,
)

data class TimeSlot(
    val sectionNumber: Int,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
)

data class CourseTimePeriod(
    val id: String = UUID.randomUUID().toString(),
    val selectedWeeks: List<Int> = defaultSelectedWeeks(),
    val dayOfWeek: Int = 1,
    val startSection: Int = 1,
    val endSection: Int = 1,
    val teacher: String = "",
    val location: String = "",
)

data class CourseFormState(
    val groupId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val cardColor: androidx.compose.ui.graphics.Color = SchedulePastelColors.default,
    val credits: String = "",
    val note: String = "",
    val timePeriods: List<CourseTimePeriod> = listOf(CourseTimePeriod()),
)

fun ScheduleCourse.toUiCourse(): Course {
    val start = startSection.coerceAtLeast(1)
    val end = endSection.coerceAtLeast(start)
    return Course(
        id = id,
        groupId = groupId,
        name = name,
        classroom = classroom,
        teacher = teacher,
        note = note,
        dayOfWeek = dayOfWeek.coerceIn(1, 7),
        startSection = start,
        endSection = end,
        cardColor = colorFromArgbLong(cardColor),
        selectedWeeks = selectedWeeks.ifEmpty { defaultSelectedWeeks() },
    )
}

fun ScheduleTimeSlot.toUiTimeSlot(): TimeSlot = TimeSlot(
    sectionNumber = sectionNumber,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes,
)

fun CourseTimePeriodData.toUiPeriod(): CourseTimePeriod = CourseTimePeriod(
    selectedWeeks = selectedWeeks.ifEmpty { defaultSelectedWeeks() },
    dayOfWeek = dayOfWeek,
    startSection = startSection,
    endSection = endSection,
    teacher = teacher,
    location = location,
)

fun CourseFormData.toUiFormState(): CourseFormState = CourseFormState(
    groupId = groupId,
    name = name,
    cardColor = colorFromArgbLong(cardColor),
    credits = credits,
    note = note,
    timePeriods = timePeriods.map { it.toUiPeriod() },
)

fun CourseFormState.toFormData(): CourseFormData = CourseFormData(
    groupId = groupId,
    name = name.trim(),
    cardColor = cardColor.toArgbLong(),
    credits = credits.trim(),
    note = note.trim(),
    timePeriods = timePeriods.map { it.toPeriodData() },
)

fun CourseTimePeriod.toPeriodData(): CourseTimePeriodData = CourseTimePeriodData(
    selectedWeeks = selectedWeeks.sorted(),
    dayOfWeek = dayOfWeek,
    startSection = startSection,
    endSection = endSection,
    teacher = teacher,
    location = location,
)
