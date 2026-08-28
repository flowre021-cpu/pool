package com.example.pool.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pool.data.PlannerRepository
import com.example.pool.data.schedule.ScheduleRepository
import com.example.pool.ui.components.PoolDatePickerDialog
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.datePickerUtcMillisToLocalDate
import com.example.pool.util.formatAgendaPeriodHeader
import com.example.pool.util.formatWeekRangeHeader
import com.example.pool.util.formatTodayHeader
import com.example.pool.util.localDateToDatePickerUtcMillis
import java.time.LocalDate

private const val PAGE_TIMELINE = 0
private const val PAGE_WEEK_AGENDA = 1
private const val HOME_PAGE_COUNT = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    plannerRepository: PlannerRepository,
    scheduleRepository: ScheduleRepository,
    onOpenAffairs: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenPreferences: () -> Unit,
    onEditCourse: (Long) -> Unit,
    onEditEvent: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onEditOpportunity: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
) {
    val context = LocalContext.current
    val homeTabPreferences = remember { HomeTabPreferences(context) }
    val defaultTab = remember { homeTabPreferences.getDefaultTab() }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(plannerRepository, scheduleRepository),
    )
    val timelineViewModel: TimelineViewModel = viewModel(
        factory = TimelineViewModelFactory(plannerRepository),
    )

    val homeTabState = rememberHomeTabPagerState(defaultTab)

    var zoomLevel by rememberSaveable { mutableStateOf(AgendaZoomLevel.WEEK) }
    var timeLayoutMode by rememberSaveable { mutableStateOf(AgendaTimeLayoutMode.HOUR_24) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var headerWeekDates by remember { mutableStateOf<List<LocalDate>>(emptyList()) }
    val latestHeaderWeekDates = rememberUpdatedState(headerWeekDates)
    val onDisplayWeekDatesChange = remember {
        { dates: List<LocalDate> ->
            if (dates.isNotEmpty() && dates != latestHeaderWeekDates.value) {
                headerWeekDates = dates
            }
        }
    }

    val timelineHeaderDates = remember { timelineWindowDates() }

    LaunchedEffect(defaultTab) {
        prewarmNonDefaultHomeTab(defaultTab, homeViewModel, timelineViewModel)
    }

    if (showEditSheet) {
        HomeEditSheet(
            onDismiss = { showEditSheet = false },
            onOpenAffairs = onOpenAffairs,
            onOpenCourses = onOpenCourses,
            onOpenPreferences = onOpenPreferences,
        )
    }

    if (showDatePicker) {
        val anchorDate by homeViewModel.anchorDate.collectAsStateWithLifecycle()
        HomeAnchorDatePickerDialog(
            initialDate = anchorDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                homeViewModel.setAnchorDate(date)
                onDisplayWeekDatesChange(
                    visibleWeekDatesForPage(
                        homeViewModel.weekStateFor(date),
                        date,
                        zoomLevel,
                    ),
                )
                homeTabState.scrollToPage(PAGE_WEEK_AGENDA)
                showDatePicker = false
            },
        )
    }

    Scaffold(
        containerColor = PoolColors.Background,
        bottomBar = {
            when (homeTabState.currentPage) {
                PAGE_TIMELINE -> HomeTimelineBottomBar(
                    onEditClick = { showEditSheet = true },
                )
                PAGE_WEEK_AGENDA -> HomeAgendaBottomBar(
                    zoomLevel = zoomLevel,
                    timeLayoutMode = timeLayoutMode,
                    onEditClick = { showEditSheet = true },
                    onToggleZoomLevel = { zoomLevel = zoomLevel.next() },
                    onToggleTimeLayout = { timeLayoutMode = timeLayoutMode.next() },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            HomeAgendaHeader(
                headerWeekDates = headerWeekDates,
                timelineHeaderDates = timelineHeaderDates,
                showAgendaHeader = homeTabState.currentPage == PAGE_WEEK_AGENDA,
                showTimelineHeader = homeTabState.currentPage == PAGE_TIMELINE,
                visibleDays = zoomLevel.visibleDays,
            )

            HomePagerIndicator(
                currentPage = homeTabState.currentPage,
                onPageClick = { page -> homeTabState.scrollToPage(page) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            HorizontalPager(
                state = homeTabState.pagerState,
                beyondViewportPageCount = homeTabState.beyondViewportPageCount,
                userScrollEnabled = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    PAGE_TIMELINE -> HomeTimelinePageHost(
                        viewModel = timelineViewModel,
                        onEditTask = onEditTask,
                        onEditOpportunity = onEditOpportunity,
                        onEditReminder = onEditReminder,
                        modifier = Modifier.fillMaxSize(),
                    )
                    PAGE_WEEK_AGENDA -> HomeWeekAgendaPageHost(
                        viewModel = homeViewModel,
                        zoomLevel = zoomLevel,
                        timeLayoutMode = timeLayoutMode,
                        onEditCourse = onEditCourse,
                        onEditEvent = onEditEvent,
                        onOpenCalendar = { showDatePicker = true },
                        onDisplayWeekDatesChange = onDisplayWeekDatesChange,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/** 14 天时间轴：Dialog 置于 Host 顶层避免 Pager 拦截 */
@Composable
private fun HomeTimelinePageHost(
    viewModel: TimelineViewModel,
    onEditTask: (Long) -> Unit,
    onEditOpportunity: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        viewModel.activateTimelinePipeline()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showReminders by viewModel.showReminders.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    FourteenDayTimelinePage(
        uiState = uiState,
        showReminders = showReminders,
        onShowRemindersChange = viewModel::setShowReminders,
        onDayClick = { selectedDay = it },
        onEditTask = {
            selectedDay = null
            onEditTask(it)
        },
        onEditOpportunity = {
            selectedDay = null
            onEditOpportunity(it)
        },
        onEditReminder = {
            selectedDay = null
            onEditReminder(it)
        },
        modifier = modifier,
    )

    selectedDay?.let { date ->
        TimelineDayDetailDialog(
            detail = uiState.dayDetailsByDate[date]
                ?: viewModel.dayDetail(date),
            onDismiss = { selectedDay = null },
            onEditTask = {
                selectedDay = null
                onEditTask(it)
            },
            onEditOpportunity = {
                selectedDay = null
                onEditOpportunity(it)
            },
            onEditReminder = {
                selectedDay = null
                onEditReminder(it)
            },
        )
    }
}

/** 单独收集 VM 状态，避免 weekStatesByAnchor 更新时重组顶栏 */
@Composable
private fun HomeWeekAgendaPageHost(
    viewModel: HomeViewModel,
    zoomLevel: AgendaZoomLevel,
    timeLayoutMode: AgendaTimeLayoutMode,
    onEditCourse: (Long) -> Unit,
    onEditEvent: (Long) -> Unit,
    onOpenCalendar: () -> Unit,
    onDisplayWeekDatesChange: (List<LocalDate>) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        viewModel.activateAgendaPipeline()
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isAgendaLoading by viewModel.isAgendaLoading.collectAsStateWithLifecycle()
    val anchorDate by viewModel.anchorDate.collectAsStateWithLifecycle()

    SevenDayAgendaPage(
        viewModel = viewModel,
        currentWeekState = state,
        isDataLoading = isAgendaLoading,
        anchorDate = anchorDate,
        zoomLevel = zoomLevel,
        timeLayoutMode = timeLayoutMode,
        onPrefetchPagerWeeks = viewModel::prefetchPagerWeeks,
        onCourseClick = onEditCourse,
        onEventClick = onEditEvent,
        onAnchorShift = viewModel::shiftAnchorDays,
        onJumpToToday = viewModel::jumpToToday,
        onOpenCalendar = onOpenCalendar,
        onDisplayWeekDatesChange = onDisplayWeekDatesChange,
        modifier = modifier,
    )
}

@Composable
private fun HomeAgendaHeader(
    headerWeekDates: List<LocalDate>,
    timelineHeaderDates: List<LocalDate>,
    showAgendaHeader: Boolean,
    showTimelineHeader: Boolean,
    visibleDays: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = when {
            showTimelineHeader && timelineHeaderDates.isNotEmpty() ->
                formatWeekRangeHeader(timelineHeaderDates)
            showAgendaHeader && headerWeekDates.isNotEmpty() ->
                formatAgendaPeriodHeader(headerWeekDates, visibleDays)
            showAgendaHeader -> ""
            else -> formatTodayHeader()
        },
        modifier = modifier.padding(horizontal = 16.dp, vertical = 20.dp),
        style = MaterialTheme.typography.headlineSmall,
        color = PoolColors.DateHeader,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeAnchorDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localDateToDatePickerUtcMillis(initialDate),
    )

    PoolDatePickerDialog(
        onDismiss = onDismiss,
        onConfirm = {
            val picked = datePickerState.selectedDateMillis ?: return@PoolDatePickerDialog
            onConfirm(datePickerUtcMillisToLocalDate(picked))
        },
        datePickerState = datePickerState,
    )
}

@Composable
private fun HomePagerIndicator(
    currentPage: Int,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomePageLabel(
            title = "14 天时间轴",
            selected = currentPage == PAGE_TIMELINE,
            onClick = { onPageClick(PAGE_TIMELINE) },
        )
        HomePageLabel(
            title = "七天事务",
            selected = currentPage == PAGE_WEEK_AGENDA,
            onClick = { onPageClick(PAGE_WEEK_AGENDA) },
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(HOME_PAGE_COUNT) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) PoolColors.AccentPrimary
                            else PoolColors.Divider,
                        ),
                )
            }
        }
    }
}

@Composable
private fun HomePageLabel(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) PoolColors.AccentPrimary else PoolColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(
                    if (selected) PoolColors.AccentPrimary else Color.Transparent,
                ),
        )
    }
}
