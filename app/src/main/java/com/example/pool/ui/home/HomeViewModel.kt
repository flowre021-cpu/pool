package com.example.pool.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.pool.data.AffairType
import com.example.pool.data.PlannerRepository
import com.example.pool.data.schedule.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val KEY_ANCHOR_EPOCH_DAY = "anchorDateEpochDay"
private const val HOME_FLOW_STOP_TIMEOUT_MS = 5_000L

class HomeViewModel(
    plannerRepository: PlannerRepository,
    scheduleRepository: ScheduleRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pageCache = AgendaPageStateCache()
    private var cachedInputsRevision = -1L

    private val _weekStatesByAnchor = MutableStateFlow<Map<Long, HomeUiState>>(emptyMap())
    val weekStatesByAnchor: StateFlow<Map<Long, HomeUiState>> = _weekStatesByAnchor.asStateFlow()

    private val anchorDateFlow = savedStateHandle.getStateFlow(
        KEY_ANCHOR_EPOCH_DAY,
        LocalDate.now().toEpochDay(),
    ).map { LocalDate.ofEpochDay(it) }

    private var agendaPipelineJob: Job? = null

    val agendaInputs: StateFlow<HomeAgendaInputs> = combine(
        scheduleRepository.observeCourses(),
        scheduleRepository.observeTimeSlots(),
        scheduleRepository.observeActiveSemester(),
        plannerRepository.getByType(AffairType.EVENT),
    ) { courses, timeSlots, semester, events ->
        val scopedCourses = courses.filter { course ->
            semester == null || course.semesterId == null || course.semesterId == semester.id
        }
        HomeAgendaInputs(
            courses = scopedCourses,
            timeSlots = timeSlots,
            semester = semester,
            events = events,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(HOME_FLOW_STOP_TIMEOUT_MS),
        initialValue = HomeAgendaInputs(),
    )

    /** 当前锚点对应完整周；打开首页时优先展示此状态 */
    val uiState: StateFlow<HomeUiState> = combine(
        agendaInputs,
        anchorDateFlow,
    ) { inputs, anchorDate ->
        syncWeekStatesRevision(inputs.contentRevision())
        val state = if (!inputs.hasAgendaData()) {
            buildHomeUiState(inputs, anchorDate)
        } else {
            pageCache.getFullWeekOrBuild(inputs, anchorDate)
        }
        publishWeekState(anchorDate, state)
        state
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(HOME_FLOW_STOP_TIMEOUT_MS),
            initialValue = buildHomeUiState(HomeAgendaInputs(), LocalDate.now()),
        )

    val anchorDate: StateFlow<LocalDate> = anchorDateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(HOME_FLOW_STOP_TIMEOUT_MS),
        initialValue = LocalDate.now(),
    )

    val isAgendaLoading: StateFlow<Boolean> = agendaInputs
        .map { inputs -> !inputs.hasAgendaData() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(HOME_FLOW_STOP_TIMEOUT_MS),
            initialValue = true,
        )

    /**
     * 默认 Tab 为时间轴时，在首帧后保持七天事务管线订阅以便切 Tab 时无需冷启动。
     */
    fun activateAgendaPipeline() {
        if (agendaPipelineJob?.isActive == true) return
        agendaPipelineJob = viewModelScope.launch {
            uiState.collect { }
        }
    }

    fun weekStateFor(pageAnchor: LocalDate): HomeUiState =
        resolvePagerWeekState(
            pageAnchor = pageAnchor,
            currentWeek = uiState.value,
            weekStatesByAnchor = weekStatesByAnchor.value,
        )

    /**
     * 预取当前页相邻两周（不含中心周；中心周由 [uiState] 优先构建并 [publishWeekState]）。
     * 应由 UI 在当前周首帧之后调用，避免拖慢首屏。
     */
    fun prefetchPagerWeeks(
        displayAnchor: LocalDate,
        referencePage: Int,
        periodDays: Int,
        centerPage: Int,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val inputs = agendaInputs.value
            if (!inputs.hasAgendaData()) return@launch
            syncWeekStatesRevision(inputs.contentRevision())
            val updates = mutableMapOf<Long, HomeUiState>()
            for (offset in intArrayOf(-1, 1)) {
                val anchor = resolvePageAnchor(
                    displayAnchor = displayAnchor,
                    referencePage = referencePage,
                    pageIndex = centerPage + offset,
                    periodDays = periodDays,
                )
                val state = pageCache.getFullWeekOrBuild(inputs, anchor)
                updates[anchor.toEpochDay()] = state
            }
            _weekStatesByAnchor.update { current ->
                val newEntries = updates.filter { (key, state) -> current[key] !== state }
                if (newEntries.isEmpty()) current else current + newEntries
            }
        }
    }

    fun shiftAnchorDays(days: Int) {
        if (days == 0) return
        val current = LocalDate.ofEpochDay(savedStateHandle[KEY_ANCHOR_EPOCH_DAY] ?: LocalDate.now().toEpochDay())
        savedStateHandle[KEY_ANCHOR_EPOCH_DAY] = current.plusDays(days.toLong()).toEpochDay()
    }

    fun setAnchorDate(date: LocalDate) {
        savedStateHandle[KEY_ANCHOR_EPOCH_DAY] = date.toEpochDay()
    }

    fun jumpToToday() {
        savedStateHandle[KEY_ANCHOR_EPOCH_DAY] = LocalDate.now().toEpochDay()
    }

    private fun syncWeekStatesRevision(revision: Long) {
        if (revision != cachedInputsRevision) {
            cachedInputsRevision = revision
            _weekStatesByAnchor.value = emptyMap()
        }
    }

    private fun publishWeekState(anchor: LocalDate, state: HomeUiState) {
        if (state.weekDates.isEmpty()) return
        val key = anchor.toEpochDay()
        _weekStatesByAnchor.update { current ->
            if (current[key] === state) current else current + (key to state)
        }
    }
}

data class HomeUiState(
    val anchorDate: LocalDate = LocalDate.now(),
    val weekNumber: Int? = null,
    val semesterName: String? = null,
    val isInSemester: Boolean = false,
    val weekDates: List<LocalDate> = emptyList(),
    val timeSlots: List<com.example.pool.ui.course.model.TimeSlot> = emptyList(),
    val dayColumns: List<AgendaDayColumn> = emptyList(),
)

data class AgendaDayColumn(
    val date: LocalDate,
    val dayOfWeek: Int,
    val blocks: List<AgendaGridBlock>,
)

sealed class AgendaGridBlock {
    abstract val key: String
    abstract val startMinutes: Int
    abstract val endMinutes: Int

    data class Course(
        val courseId: Long,
        val name: String,
        val location: String?,
        val teacher: String?,
        val note: String?,
        override val startMinutes: Int,
        override val endMinutes: Int,
        val color: androidx.compose.ui.graphics.Color,
    ) : AgendaGridBlock() {
        override val key: String = "course-$courseId"
    }

    data class Event(
        val affairId: Long,
        val title: String,
        val location: String?,
        override val startMinutes: Int,
        override val endMinutes: Int,
        val color: androidx.compose.ui.graphics.Color,
    ) : AgendaGridBlock() {
        override val key: String = "event-$affairId"
    }
}

internal fun eventOverlapsDay(startAt: Long?, endAt: Long?, date: LocalDate): Boolean {
    if (startAt == null) return false
    val zone = ZoneId.systemDefault()
    val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val eventEnd = endAt ?: startAt
    return startAt < dayEnd && eventEnd > dayStart
}

class HomeViewModelFactory(
    private val plannerRepository: PlannerRepository,
    private val scheduleRepository: ScheduleRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        return HomeViewModel(plannerRepository, scheduleRepository, savedStateHandle) as T
    }
}
