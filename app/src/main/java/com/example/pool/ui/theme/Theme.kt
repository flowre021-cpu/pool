package com.example.pool.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun paletteLightScheme(palette: PoolThemePalette) = lightColorScheme(
    primary = palette.accent.secondary,
    onPrimary = Color.White,
    secondary = palette.semantic.dateHeader,
    onSecondary = Color.White,
    background = PoolFixedColors.background,
    onBackground = PoolFixedColors.textPrimary,
    surface = PoolFixedColors.background,
    onSurface = PoolFixedColors.textPrimary,
    surfaceVariant = palette.surface.block,
    surfaceContainer = palette.surface.block,
    onSurfaceVariant = PoolFixedColors.textSecondary,
    outline = PoolFixedColors.divider,
)

private fun paletteDarkScheme(palette: PoolThemePalette) = darkColorScheme(
    primary = palette.accent.secondary,
    onPrimary = Color.White,
    background = PoolFixedColors.background,
    onBackground = PoolFixedColors.textPrimary,
    surface = PoolFixedColors.background,
    onSurface = PoolFixedColors.textPrimary,
    surfaceVariant = PoolFixedColors.navBar,
    outline = PoolFixedColors.divider,
)

@Composable
fun PoolTheme(
    themeId: String,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val palette = PoolThemeCatalog.byId(themeId)

    LaunchedEffect(themeId) {
        PoolColors.applyTheme(palette)
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> paletteDarkScheme(palette)
        else -> paletteLightScheme(palette)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    CompositionLocalProvider(LocalPoolThemePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
