package com.sylo.feature.auth.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import com.sylo.core.ui.R
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The row of PIN progress dots shown above the keypad. Each dot's fill pops in/out
 * with a spring as digits are entered; when [isError] flips on (wrong PIN), the row
 * shakes horizontally with a haptic tick and the dots flash the error color. While
 * [isVerifying] the filled dots pulse gently so the async hash check reads as
 * deliberate progress rather than a hang.
 */
@Composable
fun PinDots(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    isVerifying: Boolean = false,
) {
    val shake = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val amplitude = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(isError) {
        if (isError) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    -amplitude at 60
                    amplitude * 0.8f at 130
                    -amplitude * 0.5f at 200
                    amplitude * 0.3f at 270
                    -amplitude * 0.15f at 340
                    0f at 420
                },
            )
        }
    }

    val fillColor by animateColorAsState(
        targetValue = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
        label = "pinDotFill",
    )
    val ringColor by animateColorAsState(
        targetValue = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
        label = "pinDotRing",
    )

    // Gentle breathing applied to filled dots while the hash check runs.
    val pulseTransition = rememberInfiniteTransition(label = "pinVerifyPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isVerifying) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pinVerifyPulseScale",
    )

    Row(
        modifier = modifier.graphicsLayer { translationX = shake.value },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val fill by animateFloatAsState(
                targetValue = if (i < filled) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "pinDot$i",
            )
            Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(12.dp).border(1.5.dp, ringColor, CircleShape))
                Box(
                    Modifier
                        .size(12.dp)
                        .graphicsLayer {
                            val scale = fill * (if (isVerifying) pulse else 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = fill.coerceIn(0f, 1f)
                        }
                        .background(fillColor, CircleShape),
                )
            }
        }
    }
}

/**
 * Numeric PIN keypad (1–9, 0, backspace) matching the Stitch auth screens.
 * While [enabled] is false (e.g. during async PIN verification) the pad dims
 * and ignores input, so stray taps can't queue up mid-check.
 */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(' ', '0', '\b'),
    )
    val padAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        label = "keypadAlpha",
    )
    // Numeric keypads are a universal convention (phone dialers, calculators): the
    // digit grid always reads 1-2-3 / 4-5-6 / 7-8-9 left-to-right, even in RTL
    // languages. Force LTR here so Compose's automatic RTL mirroring of Row doesn't
    // reverse the digit order (which it does by default — 3-2-1 in Arabic otherwise).
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = padAlpha },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { c ->
                    Box(modifier = Modifier.weight(1f)) {
                        when (c) {
                            ' ' -> Spacer()
                            '\b' -> KeypadCell(enabled = enabled, onClick = onBackspace) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = stringResource(R.string.common_delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            else -> KeypadCell(enabled = enabled, onClick = { onDigit(c) }) {
                                Text(
                                    text = c.toString(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun Spacer() {
    Box(modifier = Modifier.fillMaxWidth().height(64.dp))
}

/** A single key: springs down to 90% while pressed, alongside the standard ripple. */
@Composable
private fun KeypadCell(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "keyPress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
