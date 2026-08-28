package com.example.pool.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/** Refreshes all widgets. Prefer [WidgetNavigation.homeIntent] via actionStartActivity to open the app. */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        WidgetRefreshScheduler.requestRefresh(context)
    }
}
