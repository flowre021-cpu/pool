package com.example.pool.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * 首页 Tab 状态。
 *
 * 两个页面只通过顶部标签切换，不需要再套一层禁用手势的 Pager。直接切换组合内容可避免
 * “标签状态已更新、Pager 动画尚未落定”导致的偶发空白页。
 */
@Composable
fun rememberHomeTabPagerState(defaultTab: HomeTab): HomeTabPagerState {
    val selectedPage = rememberSaveable {
        mutableIntStateOf(defaultTab.pageIndex)
    }
    return HomeTabPagerState(
        currentPage = selectedPage.intValue,
        scrollToPage = { page ->
            if (page == HomeTab.TIMELINE.pageIndex || page == HomeTab.WEEK_AGENDA.pageIndex) {
                selectedPage.intValue = page
            }
        },
    )
}

class HomeTabPagerState(
    val currentPage: Int,
    val scrollToPage: (Int) -> Unit,
)
