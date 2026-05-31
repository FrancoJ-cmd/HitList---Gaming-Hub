package com.hitlist.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo = Color(0xFF6C8EFF)
val IndigoDim = Color(0xFF3A4F99)
val TrendingRed = Color(0xFFFF5757)
val ScoreGreen = Color(0xFF4ADE80)
val GoldColor = Color(0xFFFFD700)
val SilverColor = Color(0xFFB0BEC5)
val BronzeColor = Color(0xFFCD7F32)
val BackgroundColor = Color(0xFF0F1117)
val SurfaceColor = Color(0xFF1A1C2A)
val SurfaceVariantColor = Color(0xFF252840)
val OutlineColor = Color(0xFF353755)
val OnSurfaceDim = Color(0xFF8891B4)

private val HitListDarkColors = darkColorScheme(
    primary = Indigo,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = IndigoDim,
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFFB4C8EE),
    onSecondary = Color(0xFF1E2D42),
    background = BackgroundColor,
    onBackground = Color(0xFFEAEBF4),
    surface = SurfaceColor,
    onSurface = Color(0xFFEAEBF4),
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = OnSurfaceDim,
    outline = OutlineColor,
    error = TrendingRed,
    onError = Color(0xFFFFFFFF)
)

@Composable
fun HitListTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HitListDarkColors,
        content = content
    )
}
