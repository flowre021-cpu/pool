package com.example.pool.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import com.example.pool.data.AffairType
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class WidgetKind {
    TodaySchedule,
    Ddl,
    Reminder,
}

internal object WidgetImmediateUpdater {
    private const val TAG = "PoolWidget"
    private val glanceLocks = ConcurrentHashMap<GlanceId, Mutex>()

    private fun lockFor(glanceId: GlanceId): Mutex =
        glanceLocks.getOrPut(glanceId) { Mutex() }

    /**
     * 通过 Glance 官方 [GlanceAppWidget.update] 重跑 [GlanceAppWidget.provideGlance] 并刷新 UI。
     * 同一 [GlanceId] 串行化，避免 compose()+updateAppWidget 与活跃 Glance 会话竞态导致整块空白/消失。
     */
    suspend fun pushUi(context: Context, glanceId: GlanceId, kind: WidgetKind) {
        lockFor(glanceId).withLock {
            WidgetTheme.sync(context)
            val widget = widgetFor(kind)
            runCatching {
                widget.update(context, glanceId)
            }.onFailure { e ->
                Log.e(TAG, "Widget update failed: $kind glanceId=$glanceId", e)
            }
        }
    }

    fun kindFor(affairType: AffairType): WidgetKind? = when (affairType) {
        AffairType.TASK -> WidgetKind.Ddl
        AffairType.REMINDER -> WidgetKind.Reminder
        else -> null
    }

    private fun widgetFor(kind: WidgetKind): GlanceAppWidget = when (kind) {
        WidgetKind.TodaySchedule -> TodayScheduleWidget()
        WidgetKind.Ddl -> DdlWidget()
        WidgetKind.Reminder -> ReminderWidget()
    }
}
