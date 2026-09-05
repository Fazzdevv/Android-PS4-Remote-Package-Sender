package com.fazzdev.ps4pkgsender.ui.theme

import androidx.compose.ui.graphics.Color

val PsBluePrimary = Color(0xFF0070D1)
val PsBlueDark = Color(0xFF00439C)
val PsCyanAccent = Color(0xFF00D2FF)
val PsNavyDark = Color(0xFF080C14)

val PsBackground = Color(0xFF0B1017)
val PsSurface = Color(0xFF141C28)
val PsSurfaceVariant = Color(0xFF1E2837)
val PsBorder = Color(0xFF2B3A4F)

val TextPrimary = Color(0xFFF0F4F8)
val TextSecondary = Color(0xFF8F9CAE)
val TextMuted = Color(0xFF5A6678)

val StatusSuccess = Color(0xFF00C853)
val StatusWarning = Color(0xFFFFAB00)
val StatusError = Color(0xFFFF5252)
val StatusInfo = Color(0xFF29B6F6)

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val cyanAccent: Color,
    val navyDark: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    background = Color(0xFF0B1017),
    surface = Color(0xFF141C28),
    surfaceVariant = Color(0xFF1E2837),
    border = Color(0xFF2B3A4F),
    textPrimary = Color(0xFFF0F4F8),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    primary = PsBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PsBlueDark,
    cyanAccent = PsCyanAccent,
    navyDark = Color(0xFF080C14),
    isDark = true
)

val LightAppColors = AppColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    border = Color(0xFFCBD5E1),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    primary = Color(0xFF0056B3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    cyanAccent = Color(0xFF0056B3),
    navyDark = Color(0xFFFFFFFF),
    isDark = false
)
