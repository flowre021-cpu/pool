package com.example.pool.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

private const val HOME_TAB_PAGE_COUNT = 2
private const val HOME_TAB_ANIM_FIRST_MS = 320
private const val HOME_TAB_ANIM_RETURN_MS = 220

/**
 * 首页 Tab Pager：首次切换前仅 compose 当前页；两 Tab 都访问后 [beyondViewportPageCount]=1 保持存活。
 */
@Composable
fun rememberHomeTabPagerState(defaultTab: HomeTab): HomeTabPagerState {
    val scope = rememberCoroutineScope()
    val pagerState = key("homeTabPagerV3") {
        rememberPagerState(
            initialPage = defaultTab.pageIndex,
            pageCount = { HOME_TAB_PAGE_COUNT },
        )
    }
    var visitedTimeline by rememberSaveable {
        mutableStateOf(defaultTab == HomeTab.TIMELINE)
    }
    var visitedWeek by rememberSaveable {
        mutableStateOf(defaultTab == HomeTab.WEEK_AGENDA)
    }
    var indicatorPage by rememberSaveable {
        mutableIntStateOf(defaultTab.pageIndex)
    }

    LaunchedEffect(defaultTab) {
        if (pagerState.currentPage !in 0 until HOME_TAB_PAGE_COUNT) {
            pagerState.scrollToPage(defaultTab.pageIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            HomeTab.TIMELINE.pageIndex -> visitedTimeline = true
            HomeTab.WEEK_AGENDA.pageIndex -> visitedWeek = true
        }
        indicatorPage = pagerState.currentPage
    }

    val bothTabsVisited = visitedTimeline && visitedWeek

    return HomeTabPagerState(
        pagerState = pagerState,
        currentPage = indicatorPage,
        beyondViewportPageCount = if (bothTabsVisited) 1 else 0,
        scrollToPage = { page ->
            indicatorPage = page
            val returning = when (page) {
                HomeTab.TIMELINE.pageIndex -> visitedTimeline
                HomeTab.WEEK_AGENDA.pageIndex -> visitedWeek
                else -> false
            }
            when (page) {
                HomeTab.TIMELINE.pageIndex -> visitedTimeline = true
                HomeTab.WEEK_AGENDA.pageIndex -> visitedWeek = true
            }
            scope.launch {
                pagerState.animateScrollToPage(
                    page = page,
                    animationSpec = tween(
                        durationMillis = if (returning) HOME_TAB_ANIM_RETURN_MS else HOME_TAB_ANIM_FIRST_MS,
                    ),
                )
            }
        },
    )
}

class HomeTabPagerState(
    val pagerState: PagerState,
    val currentPage: Int,
    val beyondViewportPageCount: Int,
    val scrollToPage: (Int) -> Unit,
)
