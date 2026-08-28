package com.example.pool.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            WidgetRefreshScheduler.refreshAllWidgets(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
