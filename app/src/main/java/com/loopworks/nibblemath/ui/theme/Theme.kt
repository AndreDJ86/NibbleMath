package com.loopworks.nibblemath.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD18A),
    onPrimary = Color(0xFF0F1A0C),
    primaryContainer = Color(0xFF24381F),
    onPrimaryContainer = Color(0xFFC8E6BF),
    secondary = Color(0xFFD9C58A),
    onSecondary = Color(0xFF211C0B),
    secondaryContainer = Color(0xFF3A3423),
    onSecondaryContainer = Color(0xFFEFE3C0),
    background = Color(0xFF12140F),
    onBackground = Color(0xFFE6E8E0),
    surface = Color(0xFF1A1D16),
    onSurface = Color(0xFFE6E8E0),
    surfaceVariant = Color(0xFF21251C),
    onSurfaceVariant = Color(0xFFB4BBAE),
    surfaceContainer = Color(0xFF20241B),
    surfaceContainerHigh = Color(0xFF2A2F24),
    outline = Color(0xFF3E453A),
    outlineVariant = Color(0xFF2A3026),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F6B33),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDE8C4),
    onPrimaryContainer = Color(0xFF12290F),
    secondary = Color(0xFF6E5A1E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE5C4),
    onSecondaryContainer = Color(0xFF2A230D),
    background = Color(0xFFF7F6F0),
    onBackground = Color(0xFF1B1E18),
    surface = Color(0xFFFCFCF8),
    onSurface = Color(0xFF1B1E18),
    surfaceVariant = Color(0xFFECEEE6),
    onSurfaceVariant = Color(0xFF454C41),
    surfaceContainer = Color(0xFFEEF0E8),
    surfaceContainerHigh = Color(0xFFE8EAE1),
    outline = Color(0xFFC2C4B8),
    outlineVariant = Color(0xFFE0E2D8),
)

@Composable
fun NibbleMathTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
