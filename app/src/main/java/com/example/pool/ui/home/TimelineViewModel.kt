package com.example.pool.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val TIMELINE_FLOW_STOP_TIMEOUT_MS = 5_000L

class TimelineViewModel(
    plannerRepository: PlannerRepository,
) : ViewModel() {

    private var timelinePipelineJob: Job? = null

    private val tasksFlow = plannerRepository.getByType(AffairType.TASK)
    private val opportunitiesFlow = plannerRepository.getByType(AffairType.OPPORTUNITY)
    private val remindersFlow = plannerRepository.getByType(AffairType.REMINDER)

    val uiState: StateFlow<TimelineUiState> = combine(
        tasksFlow,
        opportunitiesFlow,
        remindersFlow,
    ) { tasks, opportunities, reminders ->
        buildTimelineUiState(
            tasks = tasks,
            opportunities = opportunities,
            reminders = reminders,
            showReminders = true,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMELINE_FLOW_STOP_TIMEOUT_MS),
            initialValue = buildTimelineUiState(
                tasks = emptyList(),
                opportunities = emptyList(),
                reminders = emptyList(),
                showReminders = true,
            ),
        )

    /** 默认 Tab 为七天事务时，在首帧后保持时间轴管线订阅以便切 Tab 时无需冷启动。 */
    fun activateTimelinePipeline() {
        if (timelinePipelineJob?.isActive == true) return
        timelinePipelineJob = viewModelScope.launch {
            uiState.collect { }
        }
    }

    fun dayDetail(date: LocalDate): TimelineDayDetail =
        uiState.value.dayDetailsByDate[date]
            ?: TimelineDayDetail(date = date, tasks = emptyList(), opportunities = emptyList(), reminders = emptyList())
}

class TimelineViewModelFactory(
    private val plannerRepository: PlannerRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TimelineViewModel(plannerRepository) as T
    }
}
