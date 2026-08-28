package com.example.pool.ui.home

import android.content.Context

enum class HomeTab(val pageIndex: Int) {
    TIMELINE(0),
    WEEK_AGENDA(1),
    ;

    fun displayName(): String = when (this) {
        TIMELINE -> "14 天时间轴"
        WEEK_AGENDA -> "七天事务"
    }

    companion object {
        fun fromPageIndex(index: Int): HomeTab =
            entries.firstOrNull { it.pageIndex == index } ?: WEEK_AGENDA
    }
}

/** 首页默认 Tab；在编辑 Sheet 中调用 [setDefaultTab] */
class HomeTabPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDefaultTab(): HomeTab {
        val index = prefs.getInt(KEY_DEFAULT_TAB, HomeTab.WEEK_AGENDA.pageIndex)
        return HomeTab.fromPageIndex(index)
    }

    fun setDefaultTab(tab: HomeTab) {
        prefs.edit().putInt(KEY_DEFAULT_TAB, tab.pageIndex).apply()
    }

    private companion object {
        const val PREFS_NAME = "home_tab_prefs"
        const val KEY_DEFAULT_TAB = "default_tab_page"
    }
}
