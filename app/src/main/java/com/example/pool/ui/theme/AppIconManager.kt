package com.example.pool.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconManager {
    fun applySavedIcon(context: Context) {
        applyIcon(context, AppIconPreferences(context).getSelectedIconId())
    }

    fun applyIcon(context: Context, iconId: String) {
        val selected = AppIconCatalog.byId(iconId)
        val packageManager = context.applicationContext.packageManager
        AppIconCatalog.all.forEach { option ->
            val component = ComponentName(context.applicationContext, option.aliasClassName)
            val newState = if (option.id == selected.id) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            if (packageManager.getComponentEnabledSetting(component) != newState) {
                packageManager.setComponentEnabledSetting(
                    component,
                    newState,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
        AppIconPreferences(context).setSelectedIconId(selected.id)
    }
}
