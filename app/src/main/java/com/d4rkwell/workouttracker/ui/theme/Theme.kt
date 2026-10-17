package com.d4rkwell.workouttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    primaryContainer = AccentOrangeContainer,
    onPrimaryContainer = AccentOrange,
    secondary = AccentBlue,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = AccentRed,
    outline = DividerDark
)

/**
 * App theme. Dark-only by design (fits the gym/companion-app aesthetic and keeps
 * things simple) — status/nav bar edge-to-edge styling is handled by
 * [androidx.activity.enableEdgeToEdge] in MainActivity, so no manual window
 * color poking is needed here.
 */
@Composable
fun WorkoutTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
