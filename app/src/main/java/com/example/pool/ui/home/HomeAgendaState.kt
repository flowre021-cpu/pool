package com.example.pool.ui.home

import com.example.pool.data.AffairEntity
import com.example.pool.data.schedule.ScheduleCourse
import com.example.pool.data.schedule.ScheduleTimeSlot
import com.example.pool.data.schedule.Semester
import com.example.pool.ui.course.model.SchedulePastelColors
import com.example.pool.ui.course.model.toUiCourse
import com.example.pool.ui.course.model.toUiTimeSlot
import com.example.pool.util.SemesterCalendar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** 构建主页网格所需的原始数据（与锚点日期解耦，供翻页复用） */
data class HomeAgendaInputs(
    val courses: List<ScheduleCourse> = emptyList(),
    val timeSlots: List<ScheduleTimeSlot> = emptyList(),
    val semester: Semester? = null,
    val events: List<AffairEntity> = emptyList(),
) {
    fun hasAgendaData(): Boolean =
        courses.isNotEmpty() || timeSlots.isNotEmpty() || events.isNotEmpty()

    /** 课程/节次/学期/日程变化时递增，用于使页面缓存失效 */
    internal fun contentRevision(): Long {
        var revision = 0L
        revision = revision * 31 + courses.size
        for (course in courses) {
            revision = revision * 31 + course.id
            revision = revision * 31 + course.dayOfWeek
            revision = revision * 31 + course.startSection
            revision = revision * 31 + course.endSection
            revision = revision * 31 + course.cardColor
            revision = revision * 31 + course.selectedWeeks.fold(0) { acc, week -> acc * 31 + week }
            revision = revision * 31 + course.name.hashCode()
        }
        revision = revision * 31 + timeSlots.size
        for (slot in timeSlots) {
            revision = revision * 31 + slot.sectionNumber
            revision = revision * 31 + slot.startTimeMinutes
            revision = revision * 31 + slot.endTimeMinutes
        }
        revision = revision * 31 + events.size
        for (event in events) {
            revision = revision * 31 + event.id
            revision = revision * 31 + (event.startAt ?: 0L)
            revision = revision * 31 + (event.endAt ?: 0L)
            revision = revision * 31 + event.title.hashCode()
            revision = revision * 31 + (event.cardColor ?: 0L)
        }
        val activeSemester = semester
        if (activeSemester != null) {
            revision = revision * 31 + activeSemester.id.hashCode()
            revision = revision * 31 + activeSemester.startDateEpochDay
            revision = revision * 31 + activeSemester.endDateEpochDay
            revision = revision * 31 + if (activeSemester.isActive) 1 else 0
        }
        return revision
    }
}

fun buildHomeUiState(
    inputs: HomeAgendaInputs,
    anchorDate: LocalDate,
): HomeUiState {
    val semester = inputs.semester
    val isInSemester = semester?.let { SemesterCalendar.contains(it, anchorDate) } == true
    val semesterStart = semester?.let { SemesterCalendar.startDate(it) }
    val weekNumber = semester?.takeIf { isInSemester }?.let {
        SemesterCalendar.weekNumberForDate(anchorDate, SemesterCalendar.startDate(it))
    }
    val weekDates = weekNumber?.let { number ->
        semesterStart?.let { SemesterCalendar.weekDates(number, it) }
    } ?: defaultWeekDates(anchorDate)
    val slots = inputs.timeSlots.map { it.toUiTimeSlot() }.sortedBy { it.sectionNumber }
    val slotBySection = slots.associateBy { it.sectionNumber }
    val weekCourses = weekNumber?.let { number ->
        inputs.courses
            .filter { number in it.selectedWeeks }
            .map { it.toUiCourse() }
    }.orEmpty()

    val dayColumns = weekDates.map { date ->
        val dayOfWeek = date.dayOfWeek.value
        val courseBlocks = weekCourses
            .filter { it.dayOfWeek == dayOfWeek }
            .mapNotNull { course ->
                val startMinutes = slotBySection[course.startSection]?.startTimeMinutes
                    ?: fallbackMinutes(course.startSection, isStart = true)
                val endMinutes = slotBySection[course.endSection]?.endTimeMinutes
                    ?: fallbackMinutes(course.endSection, isStart = false)
                val clippedStart = startMinutes.coerceIn(DAY_START_MINUTES, DAY_END_MINUTES - 1)
                val clippedEnd = endMinutes.coerceIn(clippedStart + 1, DAY_END_MINUTES)
                if (clippedStart >= DAY_END_MINUTES) return@mapNotNull null
                AgendaGridBlock.Course(
                    courseId = course.id,
                    name = course.name,
                    location = course.classroom,
                    teacher = course.teacher,
                    note = course.note,
                    startMinutes = clippedStart,
                    endMinutes = clippedEnd,
                    color = course.cardColor,
                )
            }
        val eventBlocks = inputs.events
            .filter { eventOverlapsDay(it.startAt, it.endAt, date) }
            .mapNotNull { event ->
                val startAt = event.startAt ?: return@mapNotNull null
                val endAt = event.endAt ?: (startAt + 60 * 60 * 1000)
                val (startMinutes, endMinutes) = clipToDayMinutes(startAt, endAt, date)
                if (startMinutes >= DAY_END_MINUTES) return@mapNotNull null
                AgendaGridBlock.Event(
                    affairId = event.id,
                    title = event.title,
                    location = event.location,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    color = event.cardColor?.let { com.example.pool.ui.course.model.colorFromArgbLong(it) }
                        ?: SchedulePastelColors.pastelPalette[1],
                )
            }
        AgendaDayColumn(
            date = date,
            dayOfWeek = dayOfWeek,
            blocks = courseBlocks + eventBlocks,
        )
    }

    return HomeUiState(
        anchorDate = anchorDate,
        weekNumber = weekNumber,
        semesterName = semester?.name,
        isInSemester = isInSemester,
        weekDates = weekDates,
        timeSlots = slots,
        dayColumns = dayColumns,
    )
}

/** 按视图档位裁剪可见列，供翻页与标题使用（1/3 天不再保留隐藏列） */
fun buildAgendaPageState(
    inputs: HomeAgendaInputs,
    anchorDate: LocalDate,
    zoomLevel: AgendaZoomLevel,
): HomeUiState {
    val full = buildHomeUiState(inputs, anchorDate)
    if (zoomLevel == AgendaZoomLevel.WEEK) return full

    val today = LocalDate.now()
    val focusIndex = full.weekDates.indexOf(anchorDate).takeIf { it >= 0 }
        ?: full.weekDates.indexOf(today).takeIf { it >= 0 }
        ?: (full.weekDates.lastIndex.coerceAtLeast(0) / 2)
    val indices = visibleDayIndices(full.dayColumns.size, focusIndex, zoomLevel)
    return full.copy(
        weekDates = indices.map { full.weekDates[it] },
        dayColumns = indices.map { full.dayColumns[it] },
    )
}

/** 虚拟无限 pager：pageIndex 相对 referencePage 的偏移映射到日期 */
fun resolvePageAnchor(
    displayAnchor: LocalDate,
    referencePage: Int,
    pageIndex: Int,
    periodDays: Int,
): LocalDate = displayAnchor.plusDays(((pageIndex - referencePage) * periodDays).toLong())

/** 由完整周状态 + 页内锚点 + 视图档位，得到标题/顶栏应显示的日期列 */
fun visibleWeekDatesForPage(
    weekState: HomeUiState,
    pageAnchor: LocalDate,
    zoomLevel: AgendaZoomLevel,
): List<LocalDate> {
    if (weekState.weekDates.isEmpty()) return emptyList()
    val today = LocalDate.now()
    val focusIndex = weekState.weekDates.indexOf(pageAnchor).takeIf { it >= 0 }
        ?: weekState.weekDates.indexOf(today).takeIf { it >= 0 }
        ?: (weekState.weekDates.lastIndex.coerceAtLeast(0) / 2)
    return visibleDayIndices(weekState.dayColumns.size, focusIndex, zoomLevel)
        .map { weekState.weekDates[it] }
}

/** Pager 页锚点 → 已构建周状态；优先 map，其次当前周（同周内），否则空壳 */
fun resolvePagerWeekState(
    pageAnchor: LocalDate,
    currentWeek: HomeUiState,
    weekStatesByAnchor: Map<Long, HomeUiState>,
): HomeUiState {
    weekStatesByAnchor[pageAnchor.toEpochDay()]?.let { return it }
    if (currentWeek.weekDates.isNotEmpty()) {
        if (currentWeek.weekDates.contains(pageAnchor)) {
            return currentWeek.copy(anchorDate = pageAnchor)
        }
        if (pageAnchor == currentWeek.anchorDate) {
            return currentWeek
        }
    }
    return HomeUiState(anchorDate = pageAnchor)
}

internal fun defaultWeekDates(anchor: LocalDate): List<LocalDate> {
    val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}

private fun fallbackMinutes(section: Int, isStart: Boolean): Int {
    val base = ((section.coerceAtLeast(1) - 1) * 50) + (8 * 60)
    return if (isStart) base else base + 45
}
