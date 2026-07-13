package com.sylo.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// The Stitch design uses Geist (UI) and JetBrains Mono (amounts / PIN codes).
// Those aren't bundled yet, so we fall back to the platform sans + monospace and
// keep the exact size/weight/tracking scale. Swap in downloadable fonts later.
private val Sans = FontFamily.Default
private val Mono = FontFamily.Monospace

/** Material 3 type scale mapped from the Stitch `fontSize` tokens. */
val Typography = Typography(
    // display-lg
    displayLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-0.02).em,
    ),
    // headline-lg
    headlineLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em,
    ),
    // headline-lg-mobile
    headlineMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp,
    ),
    // headline-md
    titleLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 28.sp,
    ),
    // body-lg
    bodyLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 18.sp, lineHeight = 28.sp,
    ),
    // body-md
    bodyMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    // label-sm
    labelSmall = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
)

/** label-mono — for monetary amounts, PIN digits and other tabular figures. */
val MonoNumberStyle = TextStyle(
    fontFamily = Mono, fontWeight = FontWeight.Medium,
    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.05.em,
)
