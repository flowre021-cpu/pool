package com.example.pool.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.data.schedule.ScheduleTimeSlot
import com.example.pool.data.schedule.Semester
import com.example.pool.ui.course.model.CourseFormState
import com.example.pool.ui.course.model.toFormData
import com.example.pool.ui.course.model.toUiFormState
import com.example.pool.util.SemesterCalendar
import androidx.compose.ui.graphics.Color
import com.example.pool.ui.course.model.toArgbLong
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class CourseViewModel(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    val courses = scheduleRepository.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val timeSlots = scheduleRepository.observeTimeSlots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val semesters = scheduleRepository.observeSemesters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeSemester: StateFlow<Semester?> = scheduleRepository.observeActiveSemester()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val courseGroups = scheduleRepository.observeCourseGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun semesterStartDate(semester: Semester?): LocalDate? =
        semester?.let { SemesterCalendar.startDate(it) }

    fun currentWeekForToday(semester: Semester?): Int {
        val start = semester?.let { SemesterCalendar.startDate(it) } ?: return 1
        return SemesterCalendar.weekNumberForDate(LocalDate.now(), start)
            .coerceIn(1, SemesterCalendar.weekCount(semester))
    }

    suspend fun loadCourseForm(courseId: Long): CourseFormState? =
        scheduleRepository.getCourseForm(courseId)?.toUiFormState()

    fun saveCourseForm(form: CourseFormState, onSaved: () -> Unit) {
        viewModelScope.launch {
            scheduleRepository.saveCourseForm(form.toFormData())
            onSaved()
        }
    }

    fun deleteCourseGroup(groupId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            scheduleRepository.deleteCourseGroup(groupId)
            onDeleted()
        }
    }

    fun deleteCourseGroups(groupIds: Collection<String>, onDeleted: () -> Unit) {
        viewModelScope.launch {
            groupIds.forEach { scheduleRepository.deleteCourseGroup(it) }
            onDeleted()
        }
    }

    fun updateGroupCardColor(groupId: String, color: Color) {
        viewModelScope.launch {
            scheduleRepository.updateGroupCardColor(groupId, color.toArgbLong())
        }
    }

    fun reassignAllCourseColors(
        palette: List<Color>,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val groupIds = courseGroups.value.map { it.groupId }
            val assignments = CourseColorAssigner.assignColorsToGroups(groupIds, palette)
            scheduleRepository.reassignGroupCardColors(assignments)
            onDone()
        }
    }

    fun suggestColorForNewGroup(palette: List<Color>): Color =
        CourseColorAssigner.colorAtIndex(courseGroups.value.size, palette)

    fun updateTimeSlot(slot: ScheduleTimeSlot) {
        viewModelScope.launch {
            scheduleRepository.updateTimeSlot(slot)
        }
    }

    fun upsertSemester(semester: Semester) {
        viewModelScope.launch {
            scheduleRepository.upsertSemester(semester)
        }
    }

    fun setActiveSemester(semesterId: String) {
        viewModelScope.launch {
            scheduleRepository.setActiveSemester(semesterId)
        }
    }

    fun importFromExternal(
        json: String,
        replaceAll: Boolean = false,
        semesterId: String? = null,
        onResult: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val result = scheduleRepository.importFromExternalData(json, replaceAll, semesterId)
            onResult(result.message)
        }
    }
}

class CourseViewModelFactory(
    private val scheduleRepository: ScheduleRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CourseViewModel(scheduleRepository) as T
    }
}
