package com.fazzdev.ps4pkgsender.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = PsBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PsBlueDark,
    onPrimaryContainer = PsCyanAccent,
    secondary = PsCyanAccent,
    onSecondary = DarkAppColors.navyDark,
    background = DarkAppColors.background,
    onBackground = DarkAppColors.textPrimary,
    surface = DarkAppColors.surface,
    onSurface = DarkAppColors.textPrimary,
    surfaceVariant = DarkAppColors.surfaceVariant,
    onSurfaceVariant = DarkAppColors.textSecondary,
    outline = DarkAppColors.border,
    error = StatusError
)

private val LightColorScheme = lightColorScheme(
    primary = LightAppColors.primary,
    onPrimary = Color.White,
    primaryContainer = LightAppColors.primaryContainer,
    onPrimaryContainer = LightAppColors.primary,
    secondary = LightAppColors.cyanAccent,
    onSecondary = Color.White,
    background = LightAppColors.background,
    onBackground = LightAppColors.textPrimary,
    surface = LightAppColors.surface,
    onSurface = LightAppColors.textPrimary,
    surfaceVariant = LightAppColors.surfaceVariant,
    onSurfaceVariant = LightAppColors.textSecondary,
    outline = LightAppColors.border,
    error = StatusError
)

@Composable
fun PS4PkgSenderTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.lowercase()) {
        "light" -> false
        "system" -> isSystemDark
        else -> true // default dark
    }

    val appColors = if (isDark) DarkAppColors else LightAppColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.background.toArgb()
            window.navigationBarColor = appColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
