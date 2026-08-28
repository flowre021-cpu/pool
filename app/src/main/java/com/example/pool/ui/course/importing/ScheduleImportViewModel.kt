package com.example.pool.ui.course.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.data.schedule.Semester
import com.example.pool.data.schedule.import.BuaaScheduleFetcher
import com.example.pool.data.schedule.import.BuaaScheduleImportConfig
import com.example.pool.data.schedule.import.BuaaTermSeason
import com.example.pool.data.schedule.import.BuaaTermSelection
import com.example.pool.data.schedule.import.BuaaZhengfangParser
import com.example.pool.data.schedule.import.ImportedCourseSlot
import com.example.pool.data.schedule.import.toImportedSlots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ScheduleImportViewModel(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleImportUiState())
    val uiState: StateFlow<ScheduleImportUiState> = _uiState.asStateFlow()
    private var fetchWatchdogJob: Job? = null

    init {
        val (year, season) = BuaaTermSelection.defaultSelection()
        _uiState.update {
            it.copy(
                calendarYear = year,
                season = season,
            )
        }
    }

    fun updateCalendarYear(year: Int) {
        _uiState.update { it.copy(calendarYear = year) }
    }

    fun updateSeason(season: BuaaTermSeason) {
        _uiState.update { it.copy(season = season) }
    }

    val yearOptions: List<Int> = BuaaTermSelection.yearOptions()

    fun fetchSchedule(pageUrl: String) {
        val state = _uiState.value
        val termCode = state.termCode
        val fetchMode = BuaaTermSelection.fetchMode(state.season)
        startFetching()
        viewModelScope.launch {
            runCatching {
                val result = BuaaScheduleFetcher.fetchSchedule(
                    pageUrl = pageUrl,
                    termCode = termCode,
                    fetchMode = fetchMode,
                    onProgress = { current, total ->
                        withContext(Dispatchers.Main) {
                            onFetchProgress(current, total)
                        }
                    },
                )
                val slots = result.toImportedSlots()
                if (slots.isEmpty()) {
                    error("未解析到课程，请确认已选择 ${state.termLabel}")
                }
                val importJson = BuaaZhengfangParser.toImportJson(slots)
                finishFetching()
                _uiState.update {
                    it.copy(
                        phase = ImportPhase.Preview,
                        previewSlots = slots,
                        importJson = importJson,
                        errorMessage = null,
                        progressText = null,
                    )
                }
            }.onFailure { error ->
                finishFetching()
                _uiState.update {
                    it.copy(
                        phase = ImportPhase.Ready,
                        progressText = null,
                        errorMessage = error.message ?: "抓取失败",
                    )
                }
            }
        }
    }

    private fun startFetching() {
        fetchWatchdogJob?.cancel()
        _uiState.update {
            it.copy(
                phase = ImportPhase.Fetching,
                errorMessage = null,
                progressText = "正在连接课表接口…",
            )
        }
        fetchWatchdogJob = viewModelScope.launch {
            delay(BuaaScheduleImportConfig.FETCH_WATCHDOG_MS)
            if (_uiState.value.phase == ImportPhase.Fetching) {
                finishFetching()
                _uiState.update {
                    it.copy(
                        phase = ImportPhase.Ready,
                        progressText = null,
                        errorMessage = "抓取超时（${BuaaScheduleImportConfig.FETCH_WATCHDOG_MS / 1000} 秒）。" +
                            "请确认已登录，并在课表页停留后再试。",
                    )
                }
            }
        }
    }

    fun onFetchProgress(currentWeek: Int, totalWeeks: Int) {
        _uiState.update {
            it.copy(
                progressText = if (currentWeek <= 0) {
                    "正在连接课表接口…"
                } else if (currentWeek == 1 && totalWeeks == 1) {
                    "正在读取第 1 周并推断教学周数…"
                } else {
                    "正在抓取第 $currentWeek / $totalWeeks 周…"
                },
            )
        }
    }

    private fun finishFetching() {
        fetchWatchdogJob?.cancel()
        fetchWatchdogJob = null
    }

    fun confirmImport(onDone: (String) -> Unit) {
        val state = _uiState.value
        val json = state.importJson ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(phase = ImportPhase.Importing, errorMessage = null) }
            ensureImportSemester(state)
            val result = scheduleRepository.importFromExternalData(
                jsonString = json,
                replaceAll = true,
                semesterId = state.semesterId,
            )
            if (result.importedCount > 0) {
                scheduleRepository.setActiveSemester(state.semesterId)
                _uiState.update { it.copy(phase = ImportPhase.Done) }
                onDone(result.message)
            } else {
                _uiState.update {
                    it.copy(
                        phase = ImportPhase.Preview,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    private suspend fun ensureImportSemester(state: ScheduleImportUiState) {
        val semesters = scheduleRepository.listSemesters()
        if (semesters.any { it.id == state.semesterId }) return
        val (start, end) = defaultSemesterDates(state.calendarYear, state.season)
        scheduleRepository.upsertSemester(
            Semester(
                id = state.semesterId,
                name = state.termLabel,
                startDateEpochDay = start.toEpochDay(),
                endDateEpochDay = end.toEpochDay(),
                isActive = false,
            ),
        )
    }

    fun resetPreview() {
        _uiState.update {
            it.copy(
                phase = ImportPhase.Ready,
                previewSlots = emptyList(),
                importJson = null,
                errorMessage = null,
                progressText = null,
            )
        }
    }
}

enum class ImportPhase {
    Ready,
    Fetching,
    Preview,
    Importing,
    Done,
}

data class ScheduleImportUiState(
    val calendarYear: Int = LocalDate.now().year,
    val season: BuaaTermSeason = BuaaTermSeason.SPRING,
    val phase: ImportPhase = ImportPhase.Ready,
    val previewSlots: List<ImportedCourseSlot> = emptyList(),
    val importJson: String? = null,
    val progressText: String? = null,
    val errorMessage: String? = null,
) {
    val termCode: String get() = BuaaTermSelection.toTermCode(calendarYear, season)
    val termLabel: String get() = BuaaTermSelection.displayLabel(calendarYear, season)
    val semesterId: String get() = BuaaTermSelection.toSemesterId(calendarYear, season)
}

private fun defaultSemesterDates(
    calendarYear: Int,
    season: BuaaTermSeason,
): Pair<LocalDate, LocalDate> = when (season) {
    BuaaTermSeason.SPRING -> LocalDate.of(calendarYear, 2, 23) to LocalDate.of(calendarYear, 6, 28)
    BuaaTermSeason.SUMMER -> LocalDate.of(calendarYear, 7, 1) to LocalDate.of(calendarYear, 7, 31)
    BuaaTermSeason.AUTUMN -> LocalDate.of(calendarYear, 9, 7) to LocalDate.of(calendarYear + 1, 1, 10)
}

class ScheduleImportViewModelFactory(
    private val scheduleRepository: ScheduleRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScheduleImportViewModel(scheduleRepository) as T
    }
}
