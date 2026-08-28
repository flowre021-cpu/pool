package com.example.pool.data.schedule

import com.example.pool.data.CourseDao
import com.example.pool.data.CourseEntity
import com.example.pool.data.SemesterDao
import com.example.pool.data.SemesterEntity
import com.example.pool.data.TimeSlotDao
import com.example.pool.data.TimeSlotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import com.example.pool.util.defaultSelectedWeeks
import com.example.pool.util.syncSelectedWeeksForSemesterLength
import com.example.pool.util.SemesterCalendar
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class ScheduleRepositoryImpl(
    private val courseDao: CourseDao,
    private val timeSlotDao: TimeSlotDao,
    private val semesterDao: SemesterDao,
    appScope: CoroutineScope,
) : ScheduleRepository {

    private val sharedCourses = courseDao.getAll()
        .map { list -> list.map { it.toScheduleCourse() } }
        .shareIn(appScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val sharedTimeSlots = timeSlotDao.getAll()
        .map { list -> list.map { it.toScheduleTimeSlot() } }
        .shareIn(appScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val sharedActiveSemester = semesterDao.getActive()
        .map { it?.toSemester() }
        .shareIn(appScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override fun observeCourses(): Flow<List<ScheduleCourse>> = sharedCourses

    override fun observeTimeSlots(): Flow<List<ScheduleTimeSlot>> = sharedTimeSlots

    override fun observeSemesters(): Flow<List<Semester>> =
        semesterDao.getAll().map { list -> list.map { it.toSemester() } }

    override fun observeActiveSemester(): Flow<Semester?> = sharedActiveSemester

    override suspend fun listCourses(): List<ScheduleCourse> =
        courseDao.getAllOnce().map { it.toScheduleCourse() }

    override suspend fun listTimeSlots(): List<ScheduleTimeSlot> =
        timeSlotDao.getAllOnce().map { it.toScheduleTimeSlot() }

    override suspend fun listSemesters(): List<Semester> =
        semesterDao.getAllOnce().map { it.toSemester() }

    override suspend fun getActiveSemester(): Semester? =
        semesterDao.getActiveOnce()?.toSemester()

    override fun observeCourseGroups(): Flow<List<CourseGroupSummary>> =
        sharedCourses.map { courses ->
            courses.groupBy { it.groupId }
                .map { (groupId, rows) ->
                    val first = rows.minBy { it.id }
                    CourseGroupSummary(
                        groupId = groupId,
                        name = first.name,
                        representativeCourseId = first.id,
                        periodCount = rows.size,
                        semesterId = first.semesterId,
                    )
                }
                .sortedBy { it.name }
        }

    override suspend fun getCourseForm(courseId: Long): CourseFormData? {
        val course = courseDao.getById(courseId) ?: return null
        val group = courseDao.getByGroupId(course.groupId)
        return CourseFormData(
            groupId = course.groupId,
            name = course.name,
            cardColor = course.cardColor,
            credits = course.credits.orEmpty(),
            note = course.note.orEmpty(),
            timePeriods = group.map { it.toTimePeriodData() },
        )
    }

    override suspend fun saveCourseForm(form: CourseFormData) {
        val semesterId = semesterDao.getActiveOnce()?.id
        courseDao.replaceGroup(form.groupId, form.toEntities(semesterId))
    }

    override suspend fun deleteCourseGroup(groupId: String) {
        courseDao.deleteByGroupId(groupId)
    }

    override suspend fun updateGroupCardColor(groupId: String, color: Long) {
        courseDao.updateCardColorForGroup(groupId, color)
    }

    override suspend fun reassignGroupCardColors(assignments: Map<String, Long>) {
        assignments.forEach { (groupId, color) ->
            courseDao.updateCardColorForGroup(groupId, color)
        }
    }

    override suspend fun updateTimeSlot(slot: ScheduleTimeSlot) {
        timeSlotDao.update(
            TimeSlotEntity(
                sectionNumber = slot.sectionNumber,
                startTimeMinutes = slot.startTimeMinutes,
                endTimeMinutes = slot.endTimeMinutes,
            ),
        )
    }

    override suspend fun upsertSemester(semester: Semester) {
        val existing = semesterDao.getById(semester.id)?.toSemester()
        val oldCount = existing?.let { SemesterCalendar.weekCount(it) }
        val newCount = SemesterCalendar.weekCount(semester)
        val datesChanged = existing != null && (
            existing.startDateEpochDay != semester.startDateEpochDay ||
                existing.endDateEpochDay != semester.endDateEpochDay
            )
        semesterDao.upsertKeepingActive(semester.toEntity())
        if (existing != null && (datesChanged || oldCount != newCount)) {
            syncAllCourseSelectedWeeks(
                previousTotal = oldCount ?: newCount,
                newTotal = newCount,
            )
        }
    }

    private suspend fun syncAllCourseSelectedWeeks(previousTotal: Int, newTotal: Int) {
        courseDao.getAllOnce().forEach { course ->
            val updated = syncSelectedWeeksForSemesterLength(
                selectedWeeks = course.selectedWeeks,
                previousTotal = previousTotal,
                newTotal = newTotal,
            )
            if (updated != course.selectedWeeks.sorted()) {
                courseDao.update(course.copy(selectedWeeks = updated))
            }
        }
    }

    override suspend fun setActiveSemester(semesterId: String) {
        semesterDao.setActive(semesterId)
    }

    override suspend fun importFromExternalData(
        jsonString: String,
        replaceAll: Boolean,
        semesterId: String?,
    ): ImportResult {
        return try {
            val root = JSONObject(jsonString)
            val coursesArray = root.optJSONArray("courses") ?: JSONArray()
            val entities = mutableListOf<CourseEntity>()
            for (i in 0 until coursesArray.length()) {
                val item = coursesArray.getJSONObject(i)
                entities.add(parseExternalCourse(item, semesterId))
            }
            if (entities.isEmpty()) {
                return ImportResult(importedCount = 0, message = "未解析到课程")
            }
            when {
                replaceAll && semesterId != null -> {
                    courseDao.deleteBySemesterId(semesterId)
                    entities.groupBy { it.groupId }.forEach { (_, groupRows) ->
                        courseDao.insertAll(groupRows)
                    }
                }
                replaceAll -> {
                    courseDao.deleteAll()
                    entities.groupBy { it.groupId }.forEach { (_, groupRows) ->
                        courseDao.insertAll(groupRows)
                    }
                }
                else -> {
                    entities.groupBy { it.groupId }.forEach { (groupId, groupRows) ->
                        courseDao.replaceGroup(groupId, groupRows)
                    }
                }
            }
            ImportResult(
                importedCount = entities.size,
                message = "成功导入 ${entities.size} 条课程",
            )
        } catch (e: Exception) {
            ImportResult(importedCount = 0, message = "导入失败：${e.message}")
        }
    }

    private fun parseExternalCourse(json: JSONObject, defaultSemesterId: String?): CourseEntity {
        val groupId = json.optString("groupId").ifBlank { UUID.randomUUID().toString() }
        return CourseEntity(
            groupId = groupId,
            name = json.getString("name"),
            teacher = json.optString("teacher").ifBlank { null },
            credits = json.optString("credits").ifBlank { null },
            note = json.optString("note").ifBlank { null },
            cardColor = parseColorHex(json.optString("cardColor", "#E8D5D5")),
            selectedWeeks = parseSelectedWeeks(json),
            dayOfWeek = json.getInt("dayOfWeek"),
            startSection = json.getInt("startSection"),
            endSection = json.optInt("endSection", json.getInt("startSection")),
            location = json.optString("location").ifBlank { null },
            semesterId = json.optString("semesterId").ifBlank { defaultSemesterId },
        )
    }

    private fun parseSelectedWeeks(json: JSONObject): List<Int> {
        val arr = json.optJSONArray("selectedWeeks")
        if (arr != null && arr.length() > 0) {
            return (0 until arr.length()).map { arr.getInt(it) }.sorted()
        }
        val start = json.optInt("weekStart", 1)
        val end = json.optInt("weekEnd", 20)
        return (start..end).toList()
    }

    private fun parseColorHex(hex: String): Long {
        val cleaned = hex.trim().removePrefix("#")
        return when (cleaned.length) {
            6 -> ("FF$cleaned").toLong(16)
            8 -> cleaned.toLong(16)
            else -> 0xFFE8D5D5
        }
    }

    suspend fun seedIfEmpty() {
        timeSlotDao.seedIfEmpty(defaultTimeSlots())
        semesterDao.seedIfEmpty(
            listOf(
                SemesterEntity(
                    id = "2026-spring",
                    name = "2026春季学期",
                    startDateEpochDay = java.time.LocalDate.of(2026, 2, 23).toEpochDay(),
                    endDateEpochDay = java.time.LocalDate.of(2026, 6, 28).toEpochDay(),
                    isActive = true,
                ),
                SemesterEntity(
                    id = "2026-fall",
                    name = "2026秋季学期",
                    startDateEpochDay = java.time.LocalDate.of(2026, 9, 7).toEpochDay(),
                    endDateEpochDay = java.time.LocalDate.of(2027, 1, 10).toEpochDay(),
                    isActive = false,
                ),
            ),
        )
        ensureActiveSemester()
        courseDao.seedIfEmpty(
            listOf(
                CourseEntity(
                    groupId = "sample-math",
                    name = "高等数学",
                    teacher = "张教授",
                    note = "记得带计算器",
                    cardColor = 0xFFF0A8A8L,
                    selectedWeeks = defaultSelectedWeeks(),
                    dayOfWeek = 1,
                    startSection = 3,
                    endSection = 4,
                    location = "教三 201",
                ),
                CourseEntity(
                    groupId = "sample-english",
                    name = "大学英语",
                    teacher = "李老师",
                    cardColor = 0xFF88D4BCL,
                    selectedWeeks = defaultSelectedWeeks(),
                    dayOfWeek = 3,
                    startSection = 5,
                    endSection = 6,
                    location = "外语楼 105",
                ),
            ),
        )
    }

    /** 自动切换到包含今天的学期；假期中保留用户最后选择的学期。 */
    private suspend fun ensureActiveSemester() {
        val all = semesterDao.getAllOnce()
        if (all.isEmpty()) return
        val today = java.time.LocalDate.now().toEpochDay()
        val current = all.firstOrNull { today in it.startDateEpochDay..it.endDateEpochDay }
        when {
            current != null && !current.isActive -> semesterDao.setActive(current.id)
            all.none { it.isActive } -> semesterDao.setActive(all.first().id)
        }
    }

    companion object {
        fun defaultTimeSlots(): List<TimeSlotEntity> = listOf(
            TimeSlotEntity(1, 8 * 60, 8 * 60 + 45),
            TimeSlotEntity(2, 8 * 60 + 55, 9 * 60 + 40),
            TimeSlotEntity(3, 10 * 60, 10 * 60 + 45),
            TimeSlotEntity(4, 10 * 60 + 55, 11 * 60 + 40),
            TimeSlotEntity(5, 13 * 60 + 30, 14 * 60 + 15),
            TimeSlotEntity(6, 14 * 60 + 25, 15 * 60 + 10),
            TimeSlotEntity(7, 15 * 60 + 20, 16 * 60 + 5),
            TimeSlotEntity(8, 16 * 60 + 15, 17 * 60),
            TimeSlotEntity(9, 18 * 60 + 30, 19 * 60 + 15),
            TimeSlotEntity(10, 19 * 60 + 25, 20 * 60 + 10),
            TimeSlotEntity(11, 20 * 60 + 20, 21 * 60 + 5),
            TimeSlotEntity(12, 21 * 60 + 15, 22 * 60),
        )
    }
}

private fun CourseEntity.toScheduleCourse() = ScheduleCourse(
    id = id,
    groupId = groupId,
    name = name,
    classroom = location,
    teacher = teacher,
    note = note,
    dayOfWeek = dayOfWeek,
    startSection = startSection,
    endSection = endSection,
    cardColor = cardColor,
    selectedWeeks = selectedWeeks,
    semesterId = semesterId,
)

private fun CourseEntity.toTimePeriodData() = CourseTimePeriodData(
    selectedWeeks = selectedWeeks.ifEmpty { defaultSelectedWeeks() },
    dayOfWeek = dayOfWeek,
    startSection = startSection,
    endSection = endSection,
    teacher = teacher.orEmpty(),
    location = location.orEmpty(),
)

private fun CourseFormData.toEntities(semesterId: String?): List<CourseEntity> = timePeriods.map { period ->
    CourseEntity(
        groupId = groupId,
        name = name,
        teacher = period.teacher.ifBlank { null },
        credits = credits.ifBlank { null },
        note = note.ifBlank { null },
        cardColor = cardColor,
        selectedWeeks = period.selectedWeeks.ifEmpty { defaultSelectedWeeks() },
        dayOfWeek = period.dayOfWeek,
        startSection = period.startSection,
        endSection = period.endSection,
        location = period.location.ifBlank { null },
        semesterId = semesterId,
    )
}

private fun TimeSlotEntity.toScheduleTimeSlot() = ScheduleTimeSlot(
    sectionNumber = sectionNumber,
    startTimeMinutes = startTimeMinutes,
    endTimeMinutes = endTimeMinutes,
)

private fun SemesterEntity.toSemester() = Semester(
    id = id,
    name = name,
    startDateEpochDay = startDateEpochDay,
    endDateEpochDay = endDateEpochDay,
    isActive = isActive,
)

private fun Semester.toEntity() = SemesterEntity(
    id = id,
    name = name,
    startDateEpochDay = startDateEpochDay,
    endDateEpochDay = endDateEpochDay,
    isActive = isActive,
)
