package com.driftiq.app.presentation.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Design System Color Palette
val BackgroundPrimary = Color(0xFF0D0D1A)
val BackgroundCard = Color(0xFF1A1A35)
val BackgroundElevated = Color(0xFF22224A)

val BrandPrimary = Color(0xFF6C63FF)
val BrandSecondary = Color(0xFF9D96FF)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0D0)
val TextTertiary = Color(0xFF6B6B8A)

val Success = Color(0xFF2ECC71)
val Warning = Color(0xFFF1C40F)
val ErrorColor = Color(0xFFE74C3C)
val Info = Color(0xFF3498DB)

// Dimension Colors
val DimSleep = Color(0xFF6C63FF)
val DimSocial = Color(0xFFFF6B9D)
val DimProductivity = Color(0xFF00D4AA)
val DimEntertainment = Color(0xFFFF9F43)
val DimLearning = Color(0xFF54A0FF)

private val DriftIQColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = TextPrimary,
    primaryContainer = BackgroundElevated,
    onPrimaryContainer = TextPrimary,
    secondary = BrandSecondary,
    onSecondary = TextPrimary,
    background = BackgroundPrimary,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundElevated,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    onError = TextPrimary,
)

@Composable
fun DriftIQTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DriftIQColorScheme,
        typography = Typography(),
        content = content,
    )
}

// Risk Level Color Helper
fun riskColor(level: Int): Color = when (level) {
    0 -> Color(0xFF2ECC71)
    1 -> Color(0xFFF1C40F)
    2 -> Color(0xFFE67E22)
    3 -> Color(0xFFE74C3C)
    4 -> Color(0xFFC0392B)
    5 -> Color(0xFF7B241C)
    else -> Color(0xFF2ECC71)
}
