package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HighDensityPrimary,
    onPrimary = HighDensityOnPrimary,
    secondary = HighDensitySecondary,
    onSecondary = HighDensityOnSecondary,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    secondaryContainer = HighDensitySecondaryContainer,
    onSecondaryContainer = HighDensityOnSecondaryContainer,
    background = SlateDarkBg,
    onBackground = Color.White,
    surface = CardDarkBg,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFE6E1E5),
    error = StateRejected,
    onError = Color.White,
    outline = HighDensityOutline
)

private val LightColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = HighDensityOnPrimary,
    secondary = HighDensitySecondary,
    onSecondary = HighDensityOnSecondary,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    secondaryContainer = HighDensitySecondaryContainer,
    onSecondaryContainer = HighDensityOnSecondaryContainer,
    background = HighDensityBackground,
    onBackground = HighDensityOnBackground,
    surface = HighDensitySurface,
    onSurface = HighDensityOnSurface,
    surfaceVariant = HighDensitySurfaceVariant,
    onSurfaceVariant = HighDensityOnSurfaceVariant,
    error = StateRejected,
    onError = Color.White,
    outline = HighDensityOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
