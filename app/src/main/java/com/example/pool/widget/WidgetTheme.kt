package com.example.pool.widget

import android.content.Context
import com.example.pool.ui.theme.PoolColors
import com.example.pool.ui.theme.PoolThemeCatalog
import com.example.pool.ui.theme.ThemePreferences

/** Widget 进程内同步 App 主题到 [PoolColors]（Glance 不走 Compose Theme 树）。 */
internal object WidgetTheme {
    fun sync(context: Context) {
        PoolColors.applyTheme(
            PoolThemeCatalog.byId(
                ThemePreferences(context.applicationContext).getSelectedThemeId(),
            ),
        )
    }
}
