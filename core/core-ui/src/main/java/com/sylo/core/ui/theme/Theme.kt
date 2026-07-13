package com.sylo.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

/**
 * Sylo is a dark-only brand experience (per the Stitch design), so the theme always
 * applies the brand dark scheme rather than dynamic/system colors.
 */
@Composable
fun SyloTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SyloDarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
