package com.sylo.feature.auth.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The row of PIN progress dots shown above the keypad. Each dot's fill pops in/out
 * with a spring as digits are entered; when [isError] flips on (wrong PIN), the row
 * shakes horizontally with a haptic tick and the dots flash the error color.
 */
@Composable
fun PinDots(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
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
                            scaleX = fill
                            scaleY = fill
                            alpha = fill.coerceIn(0f, 1f)
                        }
                        .background(fillColor, CircleShape),
                )
            }
        }
    }
}

/** Numeric PIN keypad (1–9, 0, backspace) matching the Stitch auth screens. */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(' ', '0', '\b'),
    )
    Column(
        modifier = modifier.fillMaxWidth(),
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
                            '\b' -> KeypadCell(onClick = onBackspace) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            else -> KeypadCell(onClick = { onDigit(c) }) {
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

@Composable
private fun Spacer() {
    Box(modifier = Modifier.fillMaxWidth().height(64.dp))
}

/** A single key: springs down to 90% while pressed, alongside the standard ripple. */
@Composable
private fun KeypadCell(
    onClick: () -> Unit,
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
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}
