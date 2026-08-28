package com.example.pool.ui.home

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

internal const val HOME_TAB_PREWARM_DELAY_MS = 300L

/** 等待默认 Tab 首屏数据就绪（Room 首批到达或时间轴窗口已构建） */
internal suspend fun awaitDefaultTabReady(
    defaultTab: HomeTab,
    homeViewModel: HomeViewModel,
    timelineViewModel: TimelineViewModel,
) {
    when (defaultTab) {
        HomeTab.WEEK_AGENDA -> {
            snapshotFlow { homeViewModel.uiState.value.weekDates.isNotEmpty() }
                .first { it }
        }
        HomeTab.TIMELINE -> {
            snapshotFlow { timelineViewModel.uiState.value.days.isNotEmpty() }
                .first { it }
        }
    }
}

/** 默认 Tab 立即启动数据管线；首帧完成后再后台预热另一 Tab */
internal suspend fun prewarmNonDefaultHomeTab(
    defaultTab: HomeTab,
    homeViewModel: HomeViewModel,
    timelineViewModel: TimelineViewModel,
) {
    when (defaultTab) {
        HomeTab.WEEK_AGENDA -> homeViewModel.activateAgendaPipeline()
        HomeTab.TIMELINE -> timelineViewModel.activateTimelinePipeline()
    }

    awaitDefaultTabReady(defaultTab, homeViewModel, timelineViewModel)
    withFrameNanos { }
    delay(HOME_TAB_PREWARM_DELAY_MS)
    when (defaultTab) {
        HomeTab.WEEK_AGENDA -> timelineViewModel.activateTimelinePipeline()
        HomeTab.TIMELINE -> homeViewModel.activateAgendaPipeline()
    }
}
