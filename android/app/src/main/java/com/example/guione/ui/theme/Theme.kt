package com.example.guione.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = md_primaryL,
    onPrimary = md_onPrimaryL,
    primaryContainer = md_primaryContainerL,
    onPrimaryContainer = md_onPrimaryContainerL,
    secondary = md_secondaryL,
    onSecondary = md_onSecondaryL,
    secondaryContainer = md_secondaryContainerL,
    onSecondaryContainer = md_onSecondaryContainerL,
    tertiary = md_tertiaryL,
    onTertiary = md_onTertiaryL,
    tertiaryContainer = md_tertiaryContainerL,
    onTertiaryContainer = md_onTertiaryContainerL,
    error = md_errorL,
    onError = md_onErrorL,
    errorContainer = md_errorContainerL,
    onErrorContainer = md_onErrorContainerL,
    background = md_backgroundL,
    onBackground = md_onBackgroundL,
    surface = md_surfaceL,
    onSurface = md_onSurfaceL,
    surfaceVariant = md_surfaceVariantL,
    onSurfaceVariant = md_onSurfaceVariantL,
    outline = md_outlineL,
    outlineVariant = md_outlineVariantL,
    inverseSurface = md_inverseSurfaceL,
    inverseOnSurface = md_inverseOnSurfaceL,
    inversePrimary = md_inversePrimaryL,
    surfaceContainerLowest = md_surfaceContainerLowestL,
    surfaceContainerLow = md_surfaceContainerLowL,
    surfaceContainer = md_surfaceContainerL,
    surfaceContainerHigh = md_surfaceContainerHighL,
    surfaceContainerHighest = md_surfaceContainerHighestL,
    scrim = md_scrim,
)

private val DarkColors = darkColorScheme(
    primary = md_primaryD,
    onPrimary = md_onPrimaryD,
    primaryContainer = md_primaryContainerD,
    onPrimaryContainer = md_onPrimaryContainerD,
    secondary = md_secondaryD,
    onSecondary = md_onSecondaryD,
    secondaryContainer = md_secondaryContainerD,
    onSecondaryContainer = md_onSecondaryContainerD,
    tertiary = md_tertiaryD,
    onTertiary = md_onTertiaryD,
    tertiaryContainer = md_tertiaryContainerD,
    onTertiaryContainer = md_onTertiaryContainerD,
    error = md_errorD,
    onError = md_onErrorD,
    errorContainer = md_errorContainerD,
    onErrorContainer = md_onErrorContainerD,
    background = md_backgroundD,
    onBackground = md_onBackgroundD,
    surface = md_surfaceD,
    onSurface = md_onSurfaceD,
    surfaceVariant = md_surfaceVariantD,
    onSurfaceVariant = md_onSurfaceVariantD,
    outline = md_outlineD,
    outlineVariant = md_outlineVariantD,
    inverseSurface = md_inverseSurfaceD,
    inverseOnSurface = md_inverseOnSurfaceD,
    inversePrimary = md_inversePrimaryD,
    surfaceContainerLowest = md_surfaceContainerLowestD,
    surfaceContainerLow = md_surfaceContainerLowD,
    surfaceContainer = md_surfaceContainerD,
    surfaceContainerHigh = md_surfaceContainerHighD,
    surfaceContainerHighest = md_surfaceContainerHighestD,
    scrim = md_scrim,
)

/**
 * The single app theme. Light and dark share layout, typography, and shapes;
 * only the color scheme differs, each a complete set of Material roles tuned for
 * its own contrast. Dynamic color is off so the research palette is consistent.
 *
 * The status- and navigation-bar icon appearance is driven by the resolved
 * theme (not just the system setting), so the icons stay readable when the user
 * switches Light/Dark/System in app.
 */
@Composable
fun ByteBiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ByteBiteTypography,
        shapes = ByteBiteShapes,
        content = content,
    )
}
