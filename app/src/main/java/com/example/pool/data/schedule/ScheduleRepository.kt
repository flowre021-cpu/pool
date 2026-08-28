package com.example.pool.data.schedule

import kotlinx.coroutines.flow.Flow

/**
 * 课表数据仓库接口 — UI 仅依赖此层，不直接访问 DAO / Entity。
 */
interface ScheduleRepository {
    fun observeCourses(): Flow<List<ScheduleCourse>>

    fun observeCourseGroups(): Flow<List<CourseGroupSummary>>

    fun observeTimeSlots(): Flow<List<ScheduleTimeSlot>>

    fun observeSemesters(): Flow<List<Semester>>

    fun observeActiveSemester(): Flow<Semester?>

    /** Widget / 后台直读，绕过 shareIn 缓存。 */
    suspend fun listCourses(): List<ScheduleCourse>

    suspend fun listTimeSlots(): List<ScheduleTimeSlot>

    suspend fun listSemesters(): List<Semester>

    suspend fun getActiveSemester(): Semester?

    suspend fun getCourseForm(courseId: Long): CourseFormData?

    suspend fun saveCourseForm(form: CourseFormData)

    suspend fun deleteCourseGroup(groupId: String)

    suspend fun updateGroupCardColor(groupId: String, color: Long)

    suspend fun reassignGroupCardColors(assignments: Map<String, Long>)

    suspend fun updateTimeSlot(slot: ScheduleTimeSlot)

    suspend fun upsertSemester(semester: Semester)

    suspend fun setActiveSemester(semesterId: String)

    suspend fun importFromExternalData(
        jsonString: String,
        replaceAll: Boolean = false,
        semesterId: String? = null,
    ): ImportResult
}
