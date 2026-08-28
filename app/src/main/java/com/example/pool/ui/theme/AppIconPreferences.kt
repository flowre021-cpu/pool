package com.example.pool.ui.theme

import android.content.Context

/** 持久化用户选择的 App 图标 id（对应 [AppIconCatalog.byId]） */
class AppIconPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedIconId(): String =
        prefs.getString(KEY_ICON_ID, AppIconCatalog.Default.id) ?: AppIconCatalog.Default.id

    fun setSelectedIconId(iconId: String) {
        prefs.edit().putString(KEY_ICON_ID, iconId).apply()
    }

    private companion object {
        const val PREFS_NAME = "pool_app_icon_prefs"
        const val KEY_ICON_ID = "selected_icon_id"
    }
}
