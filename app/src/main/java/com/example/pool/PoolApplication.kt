package com.example.pool

import android.app.Application
import com.example.pool.data.AppDatabase
import com.example.pool.data.PlannerRepository
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.data.schedule.ScheduleRepositoryImpl
import com.example.pool.widget.WidgetPreviewPublisher
import com.example.pool.widget.WidgetRefreshScheduler
import com.example.pool.ui.theme.AppIconManager
import com.example.pool.ui.theme.PoolColors
import com.example.pool.ui.theme.PoolThemeCatalog
import com.example.pool.ui.theme.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PoolApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var repository: PlannerRepository
        private set

    lateinit var scheduleRepository: ScheduleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = PlannerRepository(
            affairDao = database.affairDao(),
            appScope = applicationScope,
        )
        scheduleRepository = ScheduleRepositoryImpl(
            courseDao = database.courseDao(),
            timeSlotDao = database.timeSlotDao(),
            semesterDao = database.semesterDao(),
            appScope = applicationScope,
        )
        applicationScope.launch {
            (scheduleRepository as ScheduleRepositoryImpl).seedIfEmpty()
            WidgetPreviewPublisher.publishIfSupported(this@PoolApplication)
        }
        PoolColors.applyTheme(
            PoolThemeCatalog.byId(ThemePreferences(this).getSelectedThemeId()),
        )
        AppIconManager.applySavedIcon(this)
        WidgetRefreshScheduler.schedulePeriodicRefresh(this)
    }
}
