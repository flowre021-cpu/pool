package com.example.pool.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pool.ui.course.components.ScheduleSectionRowHeight
import com.example.pool.ui.course.components.ScheduleSidebarWidth
import com.example.pool.ui.course.model.TimeSlot
import com.example.pool.ui.theme.PoolColors
import com.example.pool.util.formatCurrentTimeLabel
import com.example.pool.util.formatMinutesOfDay
import com.example.pool.util.dayOfWeekShort
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

private val AgendaMinBlockHeight = 24.dp
private val AgendaSidebarGap = 4.dp

/**
 * 用大范围虚拟页实现近似无限翻页。
 *
 * Pager 只会组合可见页及预取页，因此扩大 pageCount 不会同时创建所有页面；
 * 不再在每次落定后跳回中心页，避免用户看到一次手势后的二次换页。
 */
private const val PERIOD_PAGER_COUNT = 20_001
private const val PERIOD_PAGER_CENTER = PERIOD_PAGER_COUNT / 2

@Composable
fun SevenDayAgendaPage(
    viewModel: HomeViewModel,
    currentWeekState: HomeUiState,
    isDataLoading: Boolean,
    anchorDate: LocalDate,
    zoomLevel: AgendaZoomLevel,
    timeLayoutMode: AgendaTimeLayoutMode,
    onPrefetchPagerWeeks: (
        displayAnchor: LocalDate,
        referencePage: Int,
        periodDays: Int,
        centerPage: Int,
    ) -> Unit,
    onCourseClick: (Long) -> Unit,
    onEventClick: (Long) -> Unit,
    onAnchorShift: (days: Int) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    onDisplayWeekDatesChange: (List<LocalDate>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val periodDays = zoomLevel.visibleDays
    val periodPager = key("periodPagerV3") {
        rememberPagerState(
            initialPage = PERIOD_PAGER_CENTER,
            pageCount = { PERIOD_PAGER_COUNT },
        )
    }
    val sharedScrollState = rememberScrollState()
    var didInitialScroll by rememberSaveable(key = "agendaDidScrollV3") { mutableStateOf(false) }
    var referencePage by rememberSaveable(key = "agendaRefPageV3") {
        mutableIntStateOf(PERIOD_PAGER_CENTER)
    }
    var displayAnchorEpoch by rememberSaveable(key = "agendaAnchorEpochV3") {
        mutableLongStateOf(anchorDate.toEpochDay())
    }
    var displayAnchor by remember {
        mutableStateOf(LocalDate.ofEpochDay(displayAnchorEpoch))
    }

    // 与 VM 锚点同一帧对齐，避免 saveable 漂移导致 pager 页拿到空 weekState
    SideEffect {
        if (displayAnchor != anchorDate) {
            displayAnchor = anchorDate
            displayAnchorEpoch = anchorDate.toEpochDay()
        }
    }

    var animateZoomColumns by remember { mutableStateOf(false) }
    var skipInitialZoomAnimation by rememberSaveable(key = "agendaSkipZoomAnimV2") {
        mutableStateOf(true)
    }

    // 防止异常恢复状态越界；V3 saveable key 会让旧版 5 页状态自然失效。
    LaunchedEffect(Unit) {
        val refInvalid = referencePage !in 0 until PERIOD_PAGER_COUNT
        val pagerInvalid = periodPager.currentPage !in 0 until PERIOD_PAGER_COUNT
        if (refInvalid || pagerInvalid) {
            referencePage = PERIOD_PAGER_CENTER
            displayAnchor = anchorDate
            displayAnchorEpoch = anchorDate.toEpochDay()
            periodPager.scrollToPage(PERIOD_PAGER_CENTER)
        }
    }

    LaunchedEffect(zoomLevel) {
        if (skipInitialZoomAnimation) {
            skipInitialZoomAnimation = false
            return@LaunchedEffect
        }
        animateZoomColumns = true
        delay(agendaMorphTotalDurationMs())
        animateZoomColumns = false
    }

    LaunchedEffect(anchorDate) {
        if (displayAnchor != anchorDate) {
            displayAnchor = anchorDate
            displayAnchorEpoch = anchorDate.toEpochDay()
            referencePage = periodPager.settledPage
        }
    }

    val currentWeekReady = currentWeekState.weekDates.isNotEmpty()

    LaunchedEffect(periodPager, periodDays) {
        snapshotFlow { periodPager.settledPage }
            .drop(1)
            .distinctUntilChanged()
            .filter { it != referencePage }
            .collect { settled ->
                val pageDelta = settled - referencePage
                val daysDelta = pageDelta * periodDays
                val newAnchor = displayAnchor.plusDays(daysDelta.toLong())
                displayAnchor = newAnchor
                displayAnchorEpoch = newAnchor.toEpochDay()
                onAnchorShift(daysDelta)
                referencePage = settled
            }
    }

    // 顶栏日期：仅在非拖动且数据就绪时更新；与预取错开一帧
    LaunchedEffect(
        displayAnchor,
        referencePage,
        periodPager.settledPage,
        periodDays,
        zoomLevel,
        currentWeekReady,
    ) {
        if (!currentWeekReady || periodPager.isScrollInProgress) return@LaunchedEffect
        withFrameNanos { }
        notifySettledHeaderWeekDates(
            displayAnchor = displayAnchor,
            referencePage = referencePage,
            settledPage = periodPager.settledPage,
            periodDays = periodDays,
            zoomLevel = zoomLevel,
            currentWeekState = currentWeekState,
            weekStatesByAnchor = viewModel.weekStatesByAnchor.value,
            onDisplayWeekDatesChange = onDisplayWeekDatesChange,
        )
    }

    // 预取邻段：拖动中跳过；顶栏更新后再过一帧，避免与 center 页同帧重组
    LaunchedEffect(
        displayAnchor,
        referencePage,
        periodDays,
        periodPager.settledPage,
        currentWeekReady,
    ) {
        if (!currentWeekReady || periodPager.isScrollInProgress) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        if (periodPager.isScrollInProgress) return@LaunchedEffect
        onPrefetchPagerWeeks(
            displayAnchor,
            referencePage,
            periodDays,
            periodPager.settledPage,
        )
    }

    HorizontalPager(
        state = periodPager,
        beyondViewportPageCount = 1,
        modifier = modifier
            .fillMaxSize()
            .background(PoolColors.Background),
    ) { page ->
        val pageAnchor = resolvePageAnchor(
            displayAnchor = displayAnchor,
            referencePage = referencePage,
            pageIndex = page,
            periodDays = periodDays,
        )
        val isCenterPage = page == referencePage
        AgendaPagerPageSlot(
            pageAnchor = pageAnchor,
            isCenterPage = isCenterPage,
            // currentPage 会在拖动越过半程时改变；settledPage 在手势结束前保持稳定，
            // 可避免垂直滚动修饰符在半程突然换页。
            isCurrentPage = page == periodPager.settledPage,
            centerWeekState = currentWeekState,
            viewModel = viewModel,
            zoomLevel = zoomLevel,
            timeLayoutMode = timeLayoutMode,
            animateZoomColumns = animateZoomColumns,
            isDataLoading = isDataLoading,
            // 翻页时保持前后页面完全一致，避免相邻页突然切到 MINIMAL 卡片。
            simplifyContent = false,
            scrollState = sharedScrollState,
            didInitialScroll = didInitialScroll,
            onInitialScrollDone = { didInitialScroll = true },
            onCourseClick = onCourseClick,
            onEventClick = onEventClick,
            onJumpToToday = onJumpToToday,
            onOpenCalendar = onOpenCalendar,
        )
    }
}

/** 按页订阅邻段缓存，避免 [weekStatesByAnchor] 更新时整棵 Pager 子树重组 */
@Composable
private fun AgendaPagerPageSlot(
    pageAnchor: LocalDate,
    isCenterPage: Boolean,
    isCurrentPage: Boolean,
    centerWeekState: HomeUiState,
    viewModel: HomeViewModel,
    zoomLevel: AgendaZoomLevel,
    timeLayoutMode: AgendaTimeLayoutMode,
    animateZoomColumns: Boolean,
    isDataLoading: Boolean,
    simplifyContent: Boolean,
    scrollState: ScrollState,
    didInitialScroll: Boolean,
    onInitialScrollDone: () -> Unit,
    onCourseClick: (Long) -> Unit,
    onEventClick: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    val anchorEpoch = pageAnchor.toEpochDay()
    val cachedCenterState = if (isCenterPage) {
        viewModel.weekStatesByAnchor.value[anchorEpoch]
    } else {
        null
    }
    val weekState = if (isCenterPage) {
        remember(centerWeekState, pageAnchor, cachedCenterState) {
            // 刚落定时 VM 的中心状态可能仍是上一页。目标页既然已完整显示，
            // 就继续使用它的缓存，避免在 VM 更新前短暂闪回上一页。
            resolvePagerWeekState(
                pageAnchor = pageAnchor,
                currentWeek = centerWeekState,
                weekStatesByAnchor = cachedCenterState?.let { mapOf(anchorEpoch to it) } ?: emptyMap(),
            )
        }
    } else {
        val cachedWeekState by produceState(
            initialValue = resolvePagerWeekState(
                pageAnchor = pageAnchor,
                currentWeek = centerWeekState,
                weekStatesByAnchor = emptyMap(),
            ),
            anchorEpoch,
            centerWeekState,
        ) {
            snapshotFlow { viewModel.weekStatesByAnchor.value[anchorEpoch] }
                .distinctUntilChanged()
                .collect { cached ->
                    value = resolvePagerWeekState(
                        pageAnchor = pageAnchor,
                        currentWeek = viewModel.uiState.value,
                        weekStatesByAnchor = cached?.let { mapOf(anchorEpoch to it) } ?: emptyMap(),
                    )
                }
        }
        cachedWeekState
    }
    if (weekState.weekDates.isEmpty() && !isCurrentPage) {
        // 目标页还没有完整缓存时保持整页空白。不要把当前页状态拼到目标页，
        // 也不要在拖动途中显示局部加载状态。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PoolColors.Background),
        )
        return
    }
    AgendaPeriodContent(
        weekState = weekState,
        pageAnchor = pageAnchor,
        zoomLevel = zoomLevel,
        timeLayoutMode = timeLayoutMode,
        animateZoomColumns = animateZoomColumns,
        isDataLoading = isDataLoading,
        simplifyContent = simplifyContent,
        scrollState = scrollState,
        ownsVerticalScroll = isCurrentPage,
        didInitialScroll = didInitialScroll,
        onInitialScrollDone = onInitialScrollDone,
        onCourseClick = onCourseClick,
        onEventClick = onEventClick,
        onJumpToToday = onJumpToToday,
        onOpenCalendar = onOpenCalendar,
    )
}

@Composable
private fun AgendaPeriodContent(
    weekState: HomeUiState,
    pageAnchor: LocalDate,
    zoomLevel: AgendaZoomLevel,
    timeLayoutMode: AgendaTimeLayoutMode,
    animateZoomColumns: Boolean,
    isDataLoading: Boolean,
    simplifyContent: Boolean,
    scrollState: ScrollState,
    ownsVerticalScroll: Boolean,
    didInitialScroll: Boolean,
    onInitialScrollDone: () -> Unit,
    onCourseClick: (Long) -> Unit,
    onEventClick: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (weekState.weekDates.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(PoolColors.Background)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isDataLoading || ownsVerticalScroll) {
                CircularProgressIndicator(color = PoolColors.AccentPrimary)
            } else {
                Text(
                    text = "暂无本周数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PoolColors.TextSecondary,
                )
            }
        }
        return
    }

    val today = LocalDate.now()
    var timeTick by remember { mutableIntStateOf(0) }
    if (!simplifyContent) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000L - (System.currentTimeMillis() % 60_000L).coerceAtLeast(1L))
                timeTick++
            }
        }
    }
    val displayState = remember(weekState, pageAnchor) {
        weekState.copy(anchorDate = pageAnchor)
    }
    val useSectionGrid = useSectionTimeGrid(timeLayoutMode, weekState.timeSlots)
    val targetGridHeight = agendaGridHeight(timeLayoutMode, weekState.timeSlots)
    val gridHeight by animateDpAsState(
        targetValue = targetGridHeight,
        animationSpec = tween(durationMillis = 200),
        label = "agendaGridHeight",
    )
    val nowY = remember(timeTick, useSectionGrid, weekState.timeSlots, targetGridHeight) {
        nowYOffset(
            useSectionGrid = useSectionGrid,
            timeSlots = weekState.timeSlots,
            sectionRowHeight = ScheduleSectionRowHeight,
        )
    }
    val nowTimeLabel = remember(timeTick) { formatCurrentTimeLabel() }
    val focusIndex = weekState.weekDates.indexOf(pageAnchor).takeIf { it >= 0 }
        ?: weekState.weekDates.indexOf(today).takeIf { it >= 0 }
        ?: todayIndexInWeek(weekState.weekDates, today)
    val visibleIndices = visibleDayIndices(weekState.dayColumns.size, focusIndex, zoomLevel)
    val monthLabel = "${weekState.weekDates.first().monthValue}月"
    val visibleCount = visibleIndices.size.coerceAtLeast(1)
    val todayColumnIndex = weekState.dayColumns.indexOfFirst { it.date == today }

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val cardsAreaWidth = maxWidth - ScheduleSidebarWidth - 1.dp - AgendaSidebarGap
        val expandedColumnWidth = (cardsAreaWidth - 4.dp) / visibleCount
        val todayInWeek = weekState.weekDates.contains(today)
        val todayBlocks = remember(todayColumnIndex, weekState.dayColumns) {
            weekState.dayColumns.getOrNull(todayColumnIndex)?.blocks?.map { block ->
                TimedLayoutEntry(
                    key = block.key,
                    startMinutes = block.startMinutes,
                    endMinutes = block.endMinutes,
                )
            } ?: emptyList()
        }
        val scrollCenterY = remember(todayBlocks, useSectionGrid, weekState.timeSlots, targetGridHeight) {
            agendaScrollCenterY(
                blocks = todayBlocks,
                useSectionGrid = useSectionGrid,
                timeSlots = weekState.timeSlots,
                sectionRowHeight = ScheduleSectionRowHeight,
            )
        }
        val gridHeightPx = with(density) { targetGridHeight.toPx() }

        LaunchedEffect(scrollCenterY, viewportHeightPx, gridHeightPx, todayInWeek, didInitialScroll) {
            if (!didInitialScroll && todayInWeek) {
                scrollState.scrollTo(
                    agendaScrollTargetPx(scrollCenterY, viewportHeightPx, gridHeightPx, density),
                )
                onInitialScrollDone()
            }
        }

        LaunchedEffect(timeLayoutMode, scrollCenterY, viewportHeightPx, gridHeightPx, todayInWeek) {
            if (!todayInWeek) return@LaunchedEffect
            delay(200)
            scrollState.scrollTo(
                agendaScrollTargetPx(scrollCenterY, viewportHeightPx, gridHeightPx, density),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AgendaSubtitleBar(
                subtitle = weekSubtitle(displayState),
                showJumpToToday = pageAnchor != today,
                onJumpToToday = onJumpToToday,
                onOpenCalendar = onOpenCalendar,
            )

            AgendaWeekHeader(
                dayColumns = weekState.dayColumns,
                visibleIndices = visibleIndices,
                expandedColumnWidth = expandedColumnWidth,
                monthLabel = monthLabel,
                today = today,
                cardsAreaWidth = cardsAreaWidth,
                animateColumns = animateZoomColumns,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .agendaSharedVerticalScroll(
                            scrollState = scrollState,
                            ownsVerticalScroll = ownsVerticalScroll,
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds(),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(ScheduleSidebarWidth)
                                .height(gridHeight)
                                .clipToBounds(),
                        ) {
                            AgendaHourSidebar(
                                gridHeight = gridHeight,
                                useSectionGrid = useSectionGrid,
                                timeSlots = weekState.timeSlots,
                            )
                            if (!simplifyContent && todayColumnIndex in visibleIndices && nowY != null) {
                                SidebarNowTimeLabel(
                                    y = nowY,
                                    label = nowTimeLabel,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(gridHeight)
                                .background(PoolColors.Divider.copy(alpha = 0.45f)),
                        )

                        Row(
                            modifier = Modifier
                                .width(cardsAreaWidth)
                                .height(gridHeight)
                                .clipToBounds()
                                .padding(start = AgendaSidebarGap, end = 4.dp),
                        ) {
                            weekState.dayColumns.forEachIndexed { index, dayColumn ->
                                val isVisible = index in visibleIndices
                                AgendaColumnSlot(
                                    isVisible = isVisible,
                                    expandedColumnWidth = expandedColumnWidth,
                                    animate = animateZoomColumns,
                                    widthLabel = "colWidth$index",
                                    alphaLabel = "colAlpha$index",
                                ) { columnWidth, columnAlpha ->
                                    AgendaDayColumn(
                                        dayColumn = dayColumn,
                                        gridHeight = gridHeight,
                                        isToday = dayColumn.date == today,
                                        zoomLevel = zoomLevel,
                                        useSectionGrid = useSectionGrid,
                                        timeSlots = weekState.timeSlots,
                                        simplifyContent = simplifyContent,
                                        nowY = if (!simplifyContent && dayColumn.date == today) nowY else null,
                                        onCourseClick = onCourseClick,
                                        onEventClick = onEventClick,
                                        modifier = Modifier
                                            .width(columnWidth)
                                            .clipToBounds()
                                            .agendaMorphFade(columnAlpha),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarNowTimeLabel(
    y: Dp,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = y - 8.dp)
            .height(16.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp,
            ),
            fontWeight = FontWeight.Bold,
            color = PoolColors.AccentPrimary,
            maxLines = 1,
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(3.dp))
                .padding(horizontal = 3.dp, vertical = 1.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(PoolColors.AccentPrimary),
        )
    }
}

@Composable
private fun CurrentTimeLine(y: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = y - 1.dp)
            .height(3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .height(3.dp)
                .background(PoolColors.AccentPrimary.copy(alpha = 0.12f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .height(1.dp)
                .background(PoolColors.AccentPrimary),
        )
    }
}

@Composable
private fun AgendaColumnSlot(
    isVisible: Boolean,
    expandedColumnWidth: Dp,
    animate: Boolean,
    widthLabel: String,
    alphaLabel: String,
    content: @Composable (columnWidth: Dp, columnAlpha: Float) -> Unit,
) {
    if (animate) {
        val columnWidth by animateDpAsState(
            targetValue = if (isVisible) expandedColumnWidth else 0.dp,
            animationSpec = agendaMorphWidthSpec(),
            label = widthLabel,
        )
        val columnAlpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = agendaMorphOpacitySpec(),
            label = alphaLabel,
        )
        if (columnWidth > 0.5.dp) {
            content(columnWidth, columnAlpha)
        }
    } else if (isVisible) {
        content(expandedColumnWidth, 1f)
    }
}

@Composable
private fun AgendaWeekHeader(
    dayColumns: List<AgendaDayColumn>,
    visibleIndices: List<Int>,
    expandedColumnWidth: Dp,
    monthLabel: String,
    today: LocalDate,
    cardsAreaWidth: Dp,
    animateColumns: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(ScheduleSidebarWidth)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.labelSmall,
                color = PoolColors.Accent,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(PoolColors.Divider.copy(alpha = 0.45f)),
        )
        Row(
            modifier = Modifier
                .width(cardsAreaWidth)
                .padding(start = AgendaSidebarGap, end = 4.dp),
        ) {
            dayColumns.forEachIndexed { index, dayColumn ->
                val isVisible = index in visibleIndices
                AgendaColumnSlot(
                    isVisible = isVisible,
                    expandedColumnWidth = expandedColumnWidth,
                    animate = animateColumns,
                    widthLabel = "headerWidth$index",
                    alphaLabel = "headerAlpha$index",
                ) { columnWidth, columnAlpha ->
                    val isToday = dayColumn.date == today
                    Column(
                        modifier = Modifier
                            .width(columnWidth)
                            .clipToBounds()
                            .agendaMorphFade(columnAlpha)
                            .padding(horizontal = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = dayOfWeekShort(dayColumn.dayOfWeek),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) PoolColors.AccentPrimary else PoolColors.Accent,
                        )
                        Text(
                            text = "${dayColumn.date.dayOfMonth}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) PoolColors.AccentPrimary else PoolColors.TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaHourSidebar(
    gridHeight: Dp,
    useSectionGrid: Boolean,
    timeSlots: List<TimeSlot>,
) {
    Crossfade(
        targetState = useSectionGrid,
        animationSpec = tween(durationMillis = 180),
        label = "agendaSidebar",
    ) { sectionGrid ->
        if (sectionGrid) {
            AgendaSectionSidebar(
                timeSlots = timeSlots,
                rowHeight = ScheduleSectionRowHeight,
                gridHeight = gridHeight,
            )
        } else {
            Column(
                modifier = Modifier
                    .width(ScheduleSidebarWidth)
                    .height(gridHeight),
            ) {
                repeat(24) { hour ->
                    Box(
                        modifier = Modifier
                            .height(AgendaHourHeight)
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            text = "%02d:00".format(hour),
                            style = MaterialTheme.typography.labelSmall,
                            color = PoolColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaSectionSidebar(
    timeSlots: List<TimeSlot>,
    rowHeight: Dp,
    gridHeight: Dp,
) {
    Column(
        modifier = Modifier
            .width(ScheduleSidebarWidth)
            .height(gridHeight),
    ) {
        timeSlots.forEach { slot ->
            Column(
                modifier = Modifier
                    .width(ScheduleSidebarWidth)
                    .height(rowHeight)
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${slot.sectionNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PoolColors.Accent,
                )
                Text(
                    text = formatMinutesOfDay(slot.startTimeMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = PoolColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = formatMinutesOfDay(slot.endTimeMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = PoolColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AgendaDayColumn(
    dayColumn: AgendaDayColumn,
    gridHeight: Dp,
    isToday: Boolean,
    zoomLevel: AgendaZoomLevel,
    useSectionGrid: Boolean,
    timeSlots: List<TimeSlot>,
    simplifyContent: Boolean,
    nowY: Dp?,
    onCourseClick: (Long) -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutEntries = remember(dayColumn.blocks) {
        dayColumn.blocks.map { block ->
            TimedLayoutEntry(
                key = block.key,
                startMinutes = block.startMinutes,
                endMinutes = block.endMinutes,
            )
        }
    }
    val positioned = remember(layoutEntries) { layoutOverlappingEntries(layoutEntries) }
    val positionByKey = remember(positioned) { positioned.associateBy { it.key } }

    BoxWithConstraints(
        modifier = modifier
            .height(gridHeight)
            .fillMaxWidth()
            .clipToBounds()
            .background(
                if (isToday) PoolColors.AccentPrimary.copy(alpha = 0.03f) else Color.Transparent,
            ),
    ) {
        val density = LocalDensity.current
        val gapPx = with(density) { AgendaOverlapGap.toPx() }
        val columnWidthPx = constraints.maxWidth.toFloat()

        val placements = remember(
            dayColumn.blocks,
            columnWidthPx,
            zoomLevel,
            simplifyContent,
            positioned,
            useSectionGrid,
            timeSlots,
        ) {
            with(density) {
                dayColumn.blocks.mapNotNull { block ->
                    val layout = positionByKey[block.key] ?: return@mapNotNull null
                    val (top, height) = if (useSectionGrid) {
                        blockGeometryWithSections(
                            startMinutes = block.startMinutes,
                            endMinutes = block.endMinutes,
                            slots = timeSlots,
                            rowHeight = ScheduleSectionRowHeight,
                            minHeight = AgendaMinBlockHeight,
                        )
                    } else {
                        blockGeometry(
                            startMinutes = block.startMinutes,
                            endMinutes = block.endMinutes,
                            minHeight = AgendaMinBlockHeight,
                        )
                    }
                    val (xPx, widthPx) = computeOverlapBounds(
                        columnWidthPx = columnWidthPx,
                        column = layout.column,
                        columnCount = layout.columnCount,
                        gapPx = gapPx,
                    )
                    val heightPx = height.toPx()
                    val topPx = top.toPx()
                    val cardWidthDp = widthPx.toDp()
                    val cardHeightDp = heightPx.toDp()
                    Pair(
                        block,
                        BlockRenderInfo(
                            top = top,
                            height = height,
                            xOffset = xPx.toDp(),
                            width = cardWidthDp,
                            xPx = xPx,
                            widthPx = widthPx,
                            topPx = topPx,
                            bottomPx = topPx + heightPx,
                            useScheduleStyle = layout.columnCount == 1,
                            cardDensity = if (simplifyContent) {
                                AgendaCardDensity.MINIMAL
                            } else {
                                resolveCardDensity(
                                    widthPx = widthPx,
                                    heightPx = heightPx,
                                    density = density,
                                    zoomLevel = zoomLevel,
                                    overlapCount = layout.columnCount,
                                )
                            },
                            showTimeRange = !simplifyContent && shouldShowEventTimeOnCard(
                                zoomLevel = zoomLevel,
                                block = block,
                                cardHeightDp = cardHeightDp,
                                cardWidthDp = cardWidthDp,
                            ),
                        ),
                    )
                }
            }
        }

        placements.forEach { (block, info) ->
            Box(
                modifier = Modifier
                    .offset(x = info.xOffset, y = info.top)
                    .width(info.width)
                    .height(info.height),
            ) {
                AgendaBlockCard(
                    block = block,
                    density = info.cardDensity,
                    useScheduleStyle = info.useScheduleStyle,
                    showTimeRange = info.showTimeRange,
                    simplifyContent = simplifyContent,
                    interactionEnabled = !simplifyContent,
                    cardWidth = info.width,
                    cardHeight = info.height,
                    zoomLevel = zoomLevel,
                    onClick = {
                        when (block) {
                            is AgendaGridBlock.Course -> onCourseClick(block.courseId)
                            is AgendaGridBlock.Event -> onEventClick(block.affairId)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (nowY != null) {
            CurrentTimeLine(y = nowY)
        }
    }
}

private data class BlockRenderInfo(
    val top: Dp,
    val height: Dp,
    val xOffset: Dp,
    val width: Dp,
    val xPx: Float,
    val widthPx: Float,
    val topPx: Float,
    val bottomPx: Float,
    val useScheduleStyle: Boolean,
    val cardDensity: AgendaCardDensity,
    val showTimeRange: Boolean,
)

@Composable
private fun AgendaSubtitleBar(
    subtitle: String,
    showJumpToToday: Boolean,
    onJumpToToday: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = PoolColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (showJumpToToday) {
            Text(
                text = "回到今天",
                style = MaterialTheme.typography.labelMedium,
                color = PoolColors.AccentPrimary,
                modifier = Modifier
                    .clickable(onClick = onJumpToToday)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        IconButton(onClick = onOpenCalendar) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "选择日期",
                tint = PoolColors.AccentPrimary,
            )
        }
    }
}

private fun notifySettledHeaderWeekDates(
    displayAnchor: LocalDate,
    referencePage: Int,
    settledPage: Int,
    periodDays: Int,
    zoomLevel: AgendaZoomLevel,
    currentWeekState: HomeUiState,
    weekStatesByAnchor: Map<Long, HomeUiState>,
    onDisplayWeekDatesChange: (List<LocalDate>) -> Unit,
) {
    val headerAnchor = resolvePageAnchor(
        displayAnchor = displayAnchor,
        referencePage = referencePage,
        pageIndex = settledPage,
        periodDays = periodDays,
    )
    val headerWeekState = resolvePagerWeekState(
        pageAnchor = headerAnchor,
        currentWeek = currentWeekState,
        weekStatesByAnchor = weekStatesByAnchor,
    )
    val headerDates = visibleWeekDatesForPage(headerWeekState, headerAnchor, zoomLevel)
    if (headerDates.isNotEmpty()) {
        onDisplayWeekDatesChange(headerDates)
    }
}

private fun todayIndexInWeek(weekDates: List<LocalDate>, today: LocalDate): Int =
    weekDates.indexOf(today).takeIf { it >= 0 }
        ?: weekDates.lastIndex.coerceAtLeast(0) / 2

private fun weekSubtitle(state: HomeUiState): String {
    val name = state.semesterName
    val weekNumber = state.weekNumber
    return when {
        state.isInSemester && name != null && weekNumber != null ->
            "$name · 第 $weekNumber 周"
        state.isInSemester && weekNumber != null -> "第 $weekNumber 周"
        name != null -> "假期 · 当前自然周"
        else -> "当前自然周"
    }
}

private fun Modifier.agendaSharedVerticalScroll(
    scrollState: ScrollState,
    ownsVerticalScroll: Boolean,
): Modifier = this.verticalScroll(
    state = scrollState,
    enabled = ownsVerticalScroll,
)
