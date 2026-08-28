package com.example.pool.widget

import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.example.pool.MainActivity
import com.example.pool.data.AffairType
import com.example.pool.ui.NavRoutes

object WidgetNavigation {
    const val EXTRA_ROUTE = "pool_widget_route"

    fun openHomeAction(context: Context): Action =
        openRouteAction(context, NavRoutes.HOME)

    fun openRouteAction(context: Context, route: String): Action =
        actionStartActivity(launchIntent(context, route))

    fun openScheduleItemAction(context: Context, item: WidgetScheduleItem): Action =
        openRouteAction(context, routeForScheduleItem(item))

    fun openAffairListAction(context: Context, type: AffairType): Action =
        openRouteAction(context, NavRoutes.affairList(type))

    fun routeForScheduleItem(item: WidgetScheduleItem): String =
        when (item.kind) {
            WidgetScheduleKind.COURSE -> NavRoutes.COURSES
            WidgetScheduleKind.EVENT -> NavRoutes.affairList(AffairType.EVENT)
        }

    fun homeIntent(context: Context): Intent = launchIntent(context, NavRoutes.HOME)

    fun launchIntent(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, route)
        }
}
