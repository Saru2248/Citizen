package com.citizenai.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    // Primary — Civic Green
    primary = CivicGreen,
    onPrimary = TextOnDark,
    primaryContainer = CivicGreenContainer,
    onPrimaryContainer = OnCivicGreenContainer,

    // Secondary — Deep Civic Blue
    secondary = DeepCivicBlue,
    onSecondary = TextOnDark,
    secondaryContainer = CivicBlueContainer,
    onSecondaryContainer = OnCivicBlueContainer,

    // Tertiary — Civic Blue Medium (used for chips, accents)
    tertiary = CivicBlueMedium,
    onTertiary = TextOnDark,
    tertiaryContainer = CivicBlueContainer,
    onTertiaryContainer = OnCivicBlueContainer,

    // Error
    error = PriorityCritical,
    onError = TextOnDark,
    errorContainer = PriorityCriticalContainer,
    onErrorContainer = PriorityCritical,

    // Background / Surface
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,

    // Outline
    outline = Divider,
    outlineVariant = Divider,
)

private val DarkColorScheme = darkColorScheme(
    primary = CivicGreenLight,
    onPrimary = DeepCivicBlue,
    primaryContainer = CivicGreenDark,
    onPrimaryContainer = CivicGreenContainer,

    secondary = CivicBlueLight,
    onSecondary = TextOnDark,
    secondaryContainer = DeepCivicBlue,
    onSecondaryContainer = TextOnDarkSecondary,

    background = BackgroundDark,
    onBackground = TextOnDark,
    surface = SurfaceDark,
    onSurface = TextOnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextOnDarkSecondary,
)

@Composable
fun CitizenAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Enable edge-to-edge rendering
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CitizenAITypography,
        content = content
    )
}
