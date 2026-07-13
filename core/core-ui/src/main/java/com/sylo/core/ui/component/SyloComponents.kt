package com.sylo.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sylo.core.ui.theme.SyloBrandCyan

/** Rounded surface-container card — the base container used all over the design. */
@Composable
fun SyloCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
            content()
        }
    }
}

/** Uppercase, tracked-out section label ("RECENT TRANSACTIONS", "SECURITY"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

/** The standard top bar: Sylo wordmark on the left, a notifications bell on the right. */
/**
 * Lets any screen's [SyloTopBar] bell open the notifications screen without every
 * feature threading a navigation callback. :app provides this around the NavDisplay.
 */
val LocalOnNotificationsClick = androidx.compose.runtime.staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun SyloTopBar(
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    onNotificationsClick: () -> Unit = LocalOnNotificationsClick.current,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SyloLogo()
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onAddClick != null) {
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Filled.Add, contentDescription = "Add expense", tint = SyloBrandCyan)
                }
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = SyloBrandCyan)
            }
        }
    }
}

/** Thin rounded budget/usage progress bar (0f..1f). */
@Composable
fun BudgetProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(SyloBrandCyan),
        )
    }
}

/** Small colored status dot + label (e.g. "Pending"). */
@Composable
fun StatusDot(label: String, color: Color = SyloBrandCyan, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
