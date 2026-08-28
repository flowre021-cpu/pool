package com.example.pool.widget

import com.example.pool.data.AffairEntity
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import com.example.pool.data.schedule.ScheduleCourse
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.data.schedule.ScheduleTimeSlot
import com.example.pool.data.schedule.Semester
import com.example.pool.ui.home.eventOverlapsDay
import com.example.pool.ui.home.reminderOccurrencesInWindow
import com.example.pool.util.isReminderOccurrenceCompleted
import com.example.pool.util.SemesterCalendar
import java.time.LocalDate
import java.time.ZoneId

object WidgetDataLoader {
    suspend fun load(
        plannerRepository: PlannerRepository,
        scheduleRepository: ScheduleRepository,
    ): WidgetSnapshot {
        val today = LocalDate.now()
        val tasks = plannerRepository.listByType(AffairType.TASK)
        val events = plannerRepository.listByType(AffairType.EVENT)
        val reminders = plannerRepository.listByType(AffairType.REMINDER)
        val courses = scheduleRepository.listCourses()
        val timeSlots = scheduleRepository.listTimeSlots()
        val semesters = scheduleRepository.listSemesters()
        val semester = semesters.firstOrNull { SemesterCalendar.contains(it, today) }
            ?: scheduleRepository.getActiveSemester()
        val scopedCourses = courses.filter { course ->
            semester == null || course.semesterId == null || course.semesterId == semester.id
        }

        return WidgetSnapshot(
            todayItems = buildTodayScheduleItems(
                today = today,
                courses = scopedCourses,
                timeSlots = timeSlots,
                semester = semester,
                events = events,
            ),
            deadlineItems = buildDeadlineItems(tasks),
            reminderItems = buildReminderItems(reminders, today),
        )
    }

    suspend fun loadDeadlineItems(plannerRepository: PlannerRepository): List<WidgetDeadlineItem> =
        buildDeadlineItems(plannerRepository.listByType(AffairType.TASK))

    suspend fun loadReminderItems(plannerRepository: PlannerRepository): List<WidgetReminderItem> =
        buildReminderItems(
            plannerRepository.listByType(AffairType.REMINDER),
            LocalDate.now(),
        )

    internal fun buildTodayScheduleItems(
        today: LocalDate,
        courses: List<ScheduleCourse>,
        timeSlots: List<ScheduleTimeSlot>,
        semester: Semester?,
        events: List<AffairEntity>,
    ): List<WidgetScheduleItem> {
        val slotBySection = timeSlots.associateBy { it.sectionNumber }
        val dayOfWeek = today.dayOfWeek.value
        val weekNumber = semester?.takeIf { SemesterCalendar.contains(it, today) }?.let {
            SemesterCalendar.weekNumberForDate(today, SemesterCalendar.startDate(it))
        }

        val courseItems = if (weekNumber != null) {
            courses
                .filter { it.dayOfWeek == dayOfWeek && weekNumber in it.selectedWeeks }
                .mapNotNull { course ->
                    val startMinutes = slotBySection[course.startSection]?.startTimeMinutes
                        ?: fallbackMinutes(course.startSection, isStart = true)
                    val endMinutes = slotBySection[course.endSection]?.endTimeMinutes
                        ?: fallbackMinutes(course.endSection, isStart = false)
                    WidgetScheduleItem(
                        id = course.id,
                        kind = WidgetScheduleKind.COURSE,
                        title = course.name,
                        location = widgetLocationLabel(course.classroom),
                        startMinutes = startMinutes,
                        endMinutes = endMinutes,
                        colorArgb = course.cardColor,
                    )
                }
        } else {
            emptyList()
        }

        val eventItems = events
            .filter { !it.isDone && eventOverlapsDay(it.startAt, it.endAt, today) }
            .mapNotNull { event ->
                val startAt = event.startAt ?: return@mapNotNull null
                val endAt = event.endAt ?: (startAt + 60 * 60 * 1000)
                val (startMinutes, endMinutes) = clipToDayMinutes(startAt, endAt, today)
                WidgetScheduleItem(
                    id = event.id,
                    kind = WidgetScheduleKind.EVENT,
                    title = event.title,
                    location = widgetLocationLabel(event.location),
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    colorArgb = event.cardColor ?: 0xFF88D4BCL,
                )
            }

        return (courseItems + eventItems).sortedWith(
            compareBy<WidgetScheduleItem> { it.startMinutes }
                .thenBy { it.endMinutes }
                .thenBy { it.id },
        )
    }

    internal fun buildDeadlineItems(
        tasks: List<AffairEntity>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<WidgetDeadlineItem> =
        tasks
            .asSequence()
            .filter { !it.isDone && it.deadline != null }
            .sortedWith(compareBy<AffairEntity> { it.deadline!! }.thenBy { it.id })
            .map { task ->
                WidgetDeadlineItem(
                    id = task.id,
                    title = task.title,
                    remainingLabel = formatWidgetDeadlineRemaining(task.deadline!!, nowMillis),
                    deadlineMillis = task.deadline!!,
                )
            }
            .toList()

    internal fun buildReminderItems(
        reminders: List<AffairEntity>,
        today: LocalDate,
    ): List<WidgetReminderItem> =
        reminders
            .asSequence()
            .filter { reminder ->
                reminderOccurrencesInWindow(reminder, listOf(today), today).contains(today)
            }
            .filter { reminder -> !isReminderOccurrenceCompleted(reminder, today) }
            .map { reminder ->
                WidgetReminderItem(
                    id = reminder.id,
                    title = reminder.title,
                    startMinutes = reminderStartMinutes(reminder.startAt),
                )
            }
            .sortedWith(
                compareBy<WidgetReminderItem> { it.startMinutes }.thenBy { it.id },
            )
            .toList()

    private fun reminderStartMinutes(startAt: Long?): Int {
        if (startAt == null) return 0
        val zone = ZoneId.systemDefault()
        val zdt = java.time.Instant.ofEpochMilli(startAt).atZone(zone)
        return zdt.hour * 60 + zdt.minute
    }

    private fun fallbackMinutes(section: Int, isStart: Boolean): Int {
        val base = ((section.coerceAtLeast(1) - 1) * 50) + (8 * 60)
        return if (isStart) base else base + 45
    }

    private fun clipToDayMinutes(startAt: Long, endAt: Long, date: LocalDate): Pair<Int, Int> {
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val clippedStart = maxOf(startAt, dayStart)
        val clippedEnd = minOf(endAt, dayEnd)
        fun toMinutes(millis: Long): Int {
            val zdt = java.time.Instant.ofEpochMilli(millis).atZone(zone)
            return zdt.hour * 60 + zdt.minute
        }
        val startMinutes = toMinutes(clippedStart).coerceAtLeast(0)
        val endMinutes = toMinutes(clippedEnd).coerceAtLeast(startMinutes + 1)
        return startMinutes to endMinutes
    }
}
