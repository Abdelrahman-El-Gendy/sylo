package com.sylo.core.ui.theme

import androidx.compose.ui.graphics.Color

/** The vivid brand cyan (logo, key CTAs, accents) — the one token features may use directly. */
val SyloBrandCyan = Color(0xFF00DBE9)

/** Positive/income amounts (not part of the M3 role set). */
val SyloIncomeGreen = Color(0xFF5DD6A0)

/**
 * Sylo brand palette — a Material 3 dark tonal scheme with a cyan brand tint,
 * exported 1:1 from the Stitch design (project 9776402998627439360).
 */
internal object SyloPalette {
    // Primary (cyan family)
    val Primary = Color(0xFFDBFCFF)
    val OnPrimary = Color(0xFF00363A)
    val PrimaryContainer = Color(0xFF00F0FF)
    val OnPrimaryContainer = Color(0xFF006970)
    val InversePrimary = Color(0xFF006970)

    // Secondary (neutral)
    val Secondary = Color(0xFFC6C6CA)
    val OnSecondary = Color(0xFF2F3034)
    val SecondaryContainer = Color(0xFF4A4B4F)
    val OnSecondaryContainer = Color(0xFFBBBBBF)

    // Tertiary
    val Tertiary = Color(0xFFF3F5FE)
    val OnTertiary = Color(0xFF2D3137)
    val TertiaryContainer = Color(0xFFD6D9E1)
    val OnTertiaryContainer = Color(0xFF5B5F66)

    // Error
    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)

    // Background / surface
    val Background = Color(0xFF101415)
    val OnBackground = Color(0xFFE0E3E5)
    val Surface = Color(0xFF101415)
    val OnSurface = Color(0xFFE0E3E5)
    val SurfaceVariant = Color(0xFF323537)
    val OnSurfaceVariant = Color(0xFFB9CACB)
    val SurfaceTint = Color(0xFF00DBE9)

    // Surface container tones
    val SurfaceContainerLowest = Color(0xFF0B0F10)
    val SurfaceContainerLow = Color(0xFF191C1E)
    val SurfaceContainer = Color(0xFF1D2022)
    val SurfaceContainerHigh = Color(0xFF272A2C)
    val SurfaceContainerHighest = Color(0xFF323537)
    val SurfaceBright = Color(0xFF363A3B)
    val SurfaceDim = Color(0xFF101415)

    // Outline
    val Outline = Color(0xFF849495)
    val OutlineVariant = Color(0xFF3B494B)

    // Inverse
    val InverseSurface = Color(0xFFE0E3E5)
    val InverseOnSurface = Color(0xFF2D3133)

    /** The vivid brand cyan used for logo, key CTAs and accents. */
    val BrandCyan = SyloBrandCyan
}

/**
 * Sylo light tonal scheme — a Material 3 light neutral-teal palette that mirrors the
 * dark [SyloPalette] roles, used when the user (or system) selects the light theme.
 * The brand cyan accent ([SyloBrandCyan]) is shared across both themes.
 */
internal object SyloLightPalette {
    // Primary (deep teal for contrast on light surfaces)
    val Primary = Color(0xFF006970)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF9EF0F8)
    val OnPrimaryContainer = Color(0xFF002022)
    val InversePrimary = Color(0xFF4DD9E6)

    // Secondary (neutral)
    val Secondary = Color(0xFF4A6365)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFCCE8E9)
    val OnSecondaryContainer = Color(0xFF051F21)

    // Tertiary
    val Tertiary = Color(0xFF4B607C)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFD3E4FF)
    val OnTertiaryContainer = Color(0xFF041C35)

    // Error
    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)

    // Background / surface
    val Background = Color(0xFFF5FAFB)
    val OnBackground = Color(0xFF171D1E)
    val Surface = Color(0xFFF5FAFB)
    val OnSurface = Color(0xFF171D1E)
    val SurfaceVariant = Color(0xFFDBE4E5)
    val OnSurfaceVariant = Color(0xFF3F484A)
    val SurfaceTint = Color(0xFF006970)

    // Surface container tones
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFEFF5F6)
    val SurfaceContainer = Color(0xFFE9EFF0)
    val SurfaceContainerHigh = Color(0xFFE3E9EA)
    val SurfaceContainerHighest = Color(0xFFDDE3E4)
    val SurfaceBright = Color(0xFFF5FAFB)
    val SurfaceDim = Color(0xFFD5DBDC)

    // Outline
    val Outline = Color(0xFF6F797A)
    val OutlineVariant = Color(0xFFBFC8CA)

    // Inverse
    val InverseSurface = Color(0xFF2B3133)
    val InverseOnSurface = Color(0xFFECF2F3)
}
