package com.example.pool.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pool.ui.components.PoolAppIconOptionRow
import com.example.pool.ui.components.PoolBlock1
import com.example.pool.ui.components.PoolDivider
import com.example.pool.ui.components.PoolPreferenceOptionRow
import com.example.pool.ui.components.PoolPreferenceSectionHeader
import com.example.pool.ui.theme.AppIconCatalog
import com.example.pool.ui.theme.AppIconManager
import com.example.pool.ui.theme.AppIconPreferences
import com.example.pool.ui.theme.PoolColors
import com.example.pool.ui.theme.PoolThemeCatalog
import com.example.pool.ui.theme.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPreferencesScreen(
    onBack: () -> Unit,
    onThemeChanged: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val homeTabPreferences = remember { HomeTabPreferences(context) }
    val themePreferences = remember { ThemePreferences(context) }
    val appIconPreferences = remember { AppIconPreferences(context) }
    var defaultTab by remember { mutableStateOf(homeTabPreferences.getDefaultTab()) }
    var themeId by remember { mutableStateOf(themePreferences.getSelectedThemeId()) }
    var iconId by remember { mutableStateOf(appIconPreferences.getSelectedIconId()) }

    Scaffold(
        containerColor = PoolColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("外观与偏好") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = PoolColors.AccentPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PoolColors.NavBar,
                    titleContentColor = PoolColors.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PoolBlock1(modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)) {
                Column {
                    PoolPreferenceSectionHeader(
                        title = "默认主页",
                        hint = "下次打开 App 时生效",
                    )
                    HomeTab.entries.forEachIndexed { index, tab ->
                        PoolPreferenceOptionRow(
                            label = tab.displayName(),
                            selected = defaultTab == tab,
                            onSelect = {
                                defaultTab = tab
                                homeTabPreferences.setDefaultTab(tab)
                            },
                        )
                        if (index < HomeTab.entries.lastIndex) {
                            PoolDivider()
                        }
                    }

                    PoolDivider()

                    PoolPreferenceSectionHeader(
                        title = "主题颜色",
                        hint = "选择后立即生效",
                    )
                    PoolThemeCatalog.all.forEachIndexed { index, theme ->
                        PoolPreferenceOptionRow(
                            label = theme.displayName,
                            selected = themeId == theme.id,
                            trailingColor = theme.accent.primary,
                            onSelect = {
                                themeId = theme.id
                                themePreferences.setSelectedThemeId(theme.id)
                                onThemeChanged(theme.id)
                            },
                        )
                        if (index < PoolThemeCatalog.all.lastIndex) {
                            PoolDivider()
                        }
                    }

                    PoolDivider()

                    PoolPreferenceSectionHeader(
                        title = "应用图标",
                        hint = "切换后桌面图标会立即更新",
                    )
                    AppIconCatalog.all.forEachIndexed { index, icon ->
                        PoolAppIconOptionRow(
                            label = icon.displayName,
                            selected = iconId == icon.id,
                            previewDrawableRes = icon.previewDrawableRes,
                            onSelect = {
                                iconId = icon.id
                                AppIconManager.applyIcon(context, icon.id)
                            },
                        )
                        if (index < AppIconCatalog.all.lastIndex) {
                            PoolDivider()
                        }
                    }
                }
            }
        }
    }
}
