package com.example.pool

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.example.pool.ui.theme.ThemePreferences
import androidx.lifecycle.lifecycleScope
import com.example.pool.ui.PoolApp
import com.example.pool.ui.theme.PoolTheme
import com.example.pool.widget.WidgetNavigation
import com.example.pool.widget.WidgetPreviewPublisher
import com.example.pool.widget.WidgetRefreshScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var widgetLaunchKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (readWidgetRoute(intent) != null) {
            widgetLaunchKey = 1
        }
        val app = application as PoolApplication
        enableEdgeToEdge()
        setContent {
            val themePreferences = remember { ThemePreferences(this) }
            var themeId by remember { mutableStateOf(themePreferences.getSelectedThemeId()) }
            PoolTheme(themeId = themeId) {
                PoolApp(
                    repository = app.repository,
                    scheduleRepository = app.scheduleRepository,
                    widgetRoute = readWidgetRoute(intent),
                    widgetLaunchKey = widgetLaunchKey,
                    onThemeChanged = { id ->
                        themeId = id
                        lifecycleScope.launch {
                            WidgetRefreshScheduler.refreshAllWidgets(this@MainActivity)
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetLaunchKey++
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            WidgetRefreshScheduler.requestRefresh(this@MainActivity)
            WidgetPreviewPublisher.publishIfSupported(this@MainActivity)
        }
    }

    private fun readWidgetRoute(intent: Intent?): String? =
        intent?.getStringExtra(WidgetNavigation.EXTRA_ROUTE)
}
