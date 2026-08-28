package com.example.pool.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager

object WidgetPreviewPublisher {
    private val receivers = listOf(
        TodayScheduleWidgetReceiver::class,
        DdlWidgetReceiver::class,
        ReminderWidgetReceiver::class,
    )

    suspend fun publishIfSupported(context: Context) {
        if (Build.VERSION.SDK_INT < 35) return
        val manager = GlanceAppWidgetManager(context)
        receivers.forEach { receiverClass ->
            runCatching {
                manager.setWidgetPreviews(receiverClass)
            }
        }
    }
}
