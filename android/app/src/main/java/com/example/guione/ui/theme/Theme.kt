package com.example.guione.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = NeutralSurface,
    primaryContainer = Teal90,
    onPrimaryContainer = OnTeal90,
    secondary = SlateVariant,
    onSecondary = NeutralSurface,
    background = NeutralBg,
    onBackground = Slate10,
    surface = NeutralSurface,
    onSurface = Slate10,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = SlateVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = ErrorLight,
    onError = NeutralSurface,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = OnTeal80,
    primaryContainer = Teal30,
    onPrimaryContainer = OnTeal30,
    secondary = DarkOnSurfaceVariant,
    onSecondary = DarkBg,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark,
    onError = ErrorContainerDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

/**
 * The single app theme. Light and dark share layout, typography, shapes and
 * semantics; only the color scheme differs, each tuned for its own contrast.
 * Dynamic color is deliberately off so the research palette is consistent
 * across devices.
 */
@Composable
fun ByteBiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ByteBiteTypography,
        shapes = ByteBiteShapes,
        content = content,
    )
}
