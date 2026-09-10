package com.citizenai.worker.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CivicGreen,
    onPrimary = TextOnDark,
    primaryContainer = CivicGreenContainer,
    onPrimaryContainer = OnCivicGreenContainer,

    secondary = DeepCivicBlue,
    onSecondary = TextOnDark,
    secondaryContainer = CivicBlueContainer,
    onSecondaryContainer = OnCivicBlueContainer,

    tertiary = CivicBlueMedium,
    onTertiary = TextOnDark,
    tertiaryContainer = CivicBlueContainer,
    onTertiaryContainer = OnCivicBlueContainer,

    error = PriorityCritical,
    onError = TextOnDark,
    errorContainer = PriorityCriticalContainer,
    onErrorContainer = PriorityCritical,

    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,

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

    background = DarkBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextOnDarkSecondary,
)

@Composable
fun WorkerTheme(
    darkTheme: Boolean = false, // Match CitizenAI default light preference
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
