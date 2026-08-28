package com.example.pool.ui.theme

import android.content.Context

/** 持久化用户选择的 App 主题 id（对应 [PoolThemeCatalog.byId]） */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedThemeId(): String =
        prefs.getString(KEY_THEME_ID, PoolThemeCatalog.Default.id) ?: PoolThemeCatalog.Default.id

    fun setSelectedThemeId(themeId: String) {
        prefs.edit().putString(KEY_THEME_ID, themeId).apply()
    }

    private companion object {
        const val PREFS_NAME = "pool_theme_prefs"
        const val KEY_THEME_ID = "selected_theme_id"
    }
}
