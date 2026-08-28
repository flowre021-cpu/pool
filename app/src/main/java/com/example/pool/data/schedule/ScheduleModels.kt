package com.example.pool.data.schedule

/** 数据层课程模型（不含 Compose 依赖） */
data class ScheduleCourse(
    val id: Long,
    val groupId: String,
    val name: String,
    val classroom: String?,
    val teacher: String?,
    val note: String?,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val cardColor: Long,
    val selectedWeeks: List<Int>,
    val semesterId: String? = null,
)

data class ScheduleTimeSlot(
    val sectionNumber: Int,
    val startTimeMinutes: Int,
    val endTimeMinutes: Int,
)

data class Semester(
    val id: String,
    val name: String,
    val startDateEpochDay: Long,
    val endDateEpochDay: Long,
    val isActive: Boolean,
)

data class CourseFormData(
    val groupId: String,
    val name: String,
    val cardColor: Long,
    val credits: String,
    val note: String,
    val timePeriods: List<CourseTimePeriodData>,
)

data class CourseTimePeriodData(
    val selectedWeeks: List<Int>,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
    val teacher: String,
    val location: String,
)

data class ImportResult(
    val importedCount: Int,
    val message: String,
)

/** 按 groupId 聚合后的课程摘要，供课程设置页列表使用 */
data class CourseGroupSummary(
    val groupId: String,
    val name: String,
    val representativeCourseId: Long,
    val periodCount: Int,
    val semesterId: String? = null,
)
