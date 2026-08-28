package com.example.pool.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pool.data.AffairType
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WidgetRefreshScheduler {
    private const val PERIODIC_WORK_NAME = "pool_today_widget_refresh"
    private const val DEBOUNCE_MS = 400L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()
    private var debounceJob: Job? = null
    private var pendingTarget: RefreshTarget = RefreshTarget.All

    enum class RefreshTarget {
        All,
        Ddl,
        Reminder,
    }

    fun schedulePeriodicRefresh(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** 合并 onStart / 保存 / 勾选等短时间内的重复刷新请求。 */
    fun requestRefresh(context: Context, target: RefreshTarget = RefreshTarget.All) {
        val appContext = context.applicationContext
        synchronized(this) {
            pendingTarget = mergeTarget(pendingTarget, target)
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(DEBOUNCE_MS)
                val refreshTarget = synchronized(this@WidgetRefreshScheduler) {
                    val merged = pendingTarget
                    pendingTarget = RefreshTarget.All
                    merged
                }
                performRefresh(appContext, refreshTarget)
            }
        }
    }

    fun requestRefreshForAffairType(context: Context, type: AffairType) {
        val target = when (type) {
            AffairType.TASK -> RefreshTarget.Ddl
            AffairType.REMINDER -> RefreshTarget.Reminder
            else -> return
        }
        requestRefresh(context, target)
    }

    /** 周期 Worker 等低频路径直接刷新，不走 debounce。 */
    suspend fun refreshAllWidgets(context: Context) {
        performRefresh(context.applicationContext, RefreshTarget.All)
    }

    @Deprecated(
        message = "Use requestRefreshForAffairType",
        replaceWith = ReplaceWith("requestRefreshForAffairType(context, type)"),
    )
    suspend fun refreshWidgetsForAffairType(context: Context, type: AffairType) {
        requestRefreshForAffairType(context, type)
    }

    private fun mergeTarget(current: RefreshTarget, incoming: RefreshTarget): RefreshTarget =
        when {
            current == RefreshTarget.All || incoming == RefreshTarget.All -> RefreshTarget.All
            current != incoming -> RefreshTarget.All
            else -> current
        }

    private suspend fun performRefresh(context: Context, target: RefreshTarget) {
        refreshMutex.withLock {
            when (target) {
                RefreshTarget.All -> {
                    refreshTodayScheduleWidgets(context)
                    refreshDdlWidgets(context)
                    refreshReminderWidgets(context)
                }
                RefreshTarget.Ddl -> refreshDdlWidgets(context)
                RefreshTarget.Reminder -> refreshReminderWidgets(context)
            }
        }
    }

    private suspend fun refreshTodayScheduleWidgets(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(TodayScheduleWidget::class.java).forEach { glanceId ->
            WidgetImmediateUpdater.pushUi(context, glanceId, WidgetKind.TodaySchedule)
        }
    }

    private suspend fun refreshDdlWidgets(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(DdlWidget::class.java).forEach { glanceId ->
            WidgetImmediateUpdater.pushUi(context, glanceId, WidgetKind.Ddl)
        }
    }

    private suspend fun refreshReminderWidgets(context: Context) {
        GlanceAppWidgetManager(context).getGlanceIds(ReminderWidget::class.java).forEach { glanceId ->
            WidgetImmediateUpdater.pushUi(context, glanceId, WidgetKind.Reminder)
        }
    }
}
