package com.example.pool.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NavRoutesTest {
    @Test
    fun resolveWidgetRoute_acceptsKnownRoutes() {
        assertEquals(NavRoutes.HOME, NavRoutes.resolveWidgetRoute("home"))
        assertEquals(
            NavRoutes.affairList(com.example.pool.data.AffairType.REMINDER),
            NavRoutes.resolveWidgetRoute("affair_list/REMINDER"),
        )
        assertEquals(
            NavRoutes.taskEdit(5L),
            NavRoutes.resolveWidgetRoute("task_edit/5"),
        )
    }

    @Test
    fun resolveWidgetRoute_rejectsUnknownRoutes() {
        assertEquals(NavRoutes.HOME, NavRoutes.resolveWidgetRoute("totally_unknown_route"))
        assertEquals(NavRoutes.HOME, NavRoutes.resolveWidgetRoute("affair_list/INVALID"))
    }
}
