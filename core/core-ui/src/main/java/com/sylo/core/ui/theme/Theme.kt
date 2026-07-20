package com.sylo.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SyloDarkColorScheme = darkColorScheme(
    primary = SyloPalette.Primary,
    onPrimary = SyloPalette.OnPrimary,
    primaryContainer = SyloPalette.PrimaryContainer,
    onPrimaryContainer = SyloPalette.OnPrimaryContainer,
    inversePrimary = SyloPalette.InversePrimary,
    secondary = SyloPalette.Secondary,
    onSecondary = SyloPalette.OnSecondary,
    secondaryContainer = SyloPalette.SecondaryContainer,
    onSecondaryContainer = SyloPalette.OnSecondaryContainer,
    tertiary = SyloPalette.Tertiary,
    onTertiary = SyloPalette.OnTertiary,
    tertiaryContainer = SyloPalette.TertiaryContainer,
    onTertiaryContainer = SyloPalette.OnTertiaryContainer,
    error = SyloPalette.Error,
    onError = SyloPalette.OnError,
    errorContainer = SyloPalette.ErrorContainer,
    onErrorContainer = SyloPalette.OnErrorContainer,
    background = SyloPalette.Background,
    onBackground = SyloPalette.OnBackground,
    surface = SyloPalette.Surface,
    onSurface = SyloPalette.OnSurface,
    surfaceVariant = SyloPalette.SurfaceVariant,
    onSurfaceVariant = SyloPalette.OnSurfaceVariant,
    surfaceTint = SyloPalette.SurfaceTint,
    surfaceBright = SyloPalette.SurfaceBright,
    surfaceDim = SyloPalette.SurfaceDim,
    surfaceContainerLowest = SyloPalette.SurfaceContainerLowest,
    surfaceContainerLow = SyloPalette.SurfaceContainerLow,
    surfaceContainer = SyloPalette.SurfaceContainer,
    surfaceContainerHigh = SyloPalette.SurfaceContainerHigh,
    surfaceContainerHighest = SyloPalette.SurfaceContainerHighest,
    outline = SyloPalette.Outline,
    outlineVariant = SyloPalette.OutlineVariant,
    inverseSurface = SyloPalette.InverseSurface,
    inverseOnSurface = SyloPalette.InverseOnSurface,
)

private val SyloLightColorScheme = lightColorScheme(
    primary = SyloLightPalette.Primary,
    onPrimary = SyloLightPalette.OnPrimary,
    primaryContainer = SyloLightPalette.PrimaryContainer,
    onPrimaryContainer = SyloLightPalette.OnPrimaryContainer,
    inversePrimary = SyloLightPalette.InversePrimary,
    secondary = SyloLightPalette.Secondary,
    onSecondary = SyloLightPalette.OnSecondary,
    secondaryContainer = SyloLightPalette.SecondaryContainer,
    onSecondaryContainer = SyloLightPalette.OnSecondaryContainer,
    tertiary = SyloLightPalette.Tertiary,
    onTertiary = SyloLightPalette.OnTertiary,
    tertiaryContainer = SyloLightPalette.TertiaryContainer,
    onTertiaryContainer = SyloLightPalette.OnTertiaryContainer,
    error = SyloLightPalette.Error,
    onError = SyloLightPalette.OnError,
    errorContainer = SyloLightPalette.ErrorContainer,
    onErrorContainer = SyloLightPalette.OnErrorContainer,
    background = SyloLightPalette.Background,
    onBackground = SyloLightPalette.OnBackground,
    surface = SyloLightPalette.Surface,
    onSurface = SyloLightPalette.OnSurface,
    surfaceVariant = SyloLightPalette.SurfaceVariant,
    onSurfaceVariant = SyloLightPalette.OnSurfaceVariant,
    surfaceTint = SyloLightPalette.SurfaceTint,
    surfaceBright = SyloLightPalette.SurfaceBright,
    surfaceDim = SyloLightPalette.SurfaceDim,
    surfaceContainerLowest = SyloLightPalette.SurfaceContainerLowest,
    surfaceContainerLow = SyloLightPalette.SurfaceContainerLow,
    surfaceContainer = SyloLightPalette.SurfaceContainer,
    surfaceContainerHigh = SyloLightPalette.SurfaceContainerHigh,
    surfaceContainerHighest = SyloLightPalette.SurfaceContainerHighest,
    outline = SyloLightPalette.Outline,
    outlineVariant = SyloLightPalette.OutlineVariant,
    inverseSurface = SyloLightPalette.InverseSurface,
    inverseOnSurface = SyloLightPalette.InverseOnSurface,
)

/**
 * Sylo's Material 3 theme. [darkTheme] selects the brand's dark or light scheme;
 * callers pass a resolved value from the user's theme preference (System/Light/Dark),
 * defaulting to the system setting.
 */
@Composable
fun SyloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SyloDarkColorScheme else SyloLightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
