package com.example.pool.ui

import com.example.pool.data.AffairType

object NavRoutes {
    const val HOME = "home"
    const val AFFAIRS = "affairs"
    const val AFFAIR_LIST = "affair_list/{affairType}"
    const val AFFAIR_EDIT = "affair_edit/{affairType}/{affairId}"
    const val TASK_EDIT = "task_edit/{taskId}"
    const val COURSES = "courses"
    const val COURSE_EDIT = "course_edit/{courseId}?day={day}&section={section}"
    const val SCHEDULE_SETTINGS = "schedule_settings"
    const val SCHEDULE_SEMESTERS = "schedule_semesters"
    const val SCHEDULE_COURSE_MANAGEMENT = "schedule_course_management"
    const val SCHEDULE_TIME_SLOTS = "schedule_time_slots"
    const val SCHEDULE_CARD_COLORS = "schedule_card_colors"
    const val SCHEDULE_IMPORT = "schedule_import"
    const val APP_PREFERENCES = "app_preferences"

    fun affairList(type: AffairType): String = "affair_list/${type.name}"

    fun affairEdit(type: AffairType, affairId: Long): String =
        "affair_edit/${type.name}/$affairId"

    fun taskEdit(taskId: Long): String = "task_edit/$taskId"

    fun courseEdit(
        courseId: Long,
        day: Int = -1,
        section: Int = -1,
    ): String = "course_edit/$courseId?day=$day&section=$section"

    /**
     * 校验 Widget / 外部 Intent 传入的路由，拒绝未知 destination。
     * 非法 route 回退 [HOME]。
     */
    fun resolveWidgetRoute(raw: String?): String {
        val route = raw?.trim()?.takeIf { it.isNotBlank() } ?: return HOME
        return if (isAllowedRoute(route)) route else HOME
    }

    fun parseAffairTypeOrNull(value: String): AffairType? =
        AffairType.entries.firstOrNull { it.name == value }

    private fun isAllowedRoute(route: String): Boolean = when (route) {
        HOME, AFFAIRS, COURSES, SCHEDULE_SETTINGS -> true
        else -> when {
            route.startsWith("affair_list/") ->
                parseAffairTypeOrNull(route.removePrefix("affair_list/")) != null
            AFFAIR_EDIT_ROUTE.matches(route) ->
                parseAffairTypeOrNull(AFFAIR_EDIT_ROUTE.matchEntire(route)!!.groupValues[1]) != null
            TASK_EDIT_ROUTE.matches(route) -> true
            COURSE_EDIT_ROUTE.matches(route) -> true
            else -> false
        }
    }

    private val AFFAIR_EDIT_ROUTE = Regex("^affair_edit/([A-Z_]+)/(\\d+)$")
    private val TASK_EDIT_ROUTE = Regex("^task_edit/(\\d+)$")
    private val COURSE_EDIT_ROUTE = Regex("^course_edit/(\\d+)(\\?day=-?\\d+&section=-?\\d+)?$")
}
