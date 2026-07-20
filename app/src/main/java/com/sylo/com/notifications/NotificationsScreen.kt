package com.sylo.com.notifications

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.database.notifications.NotificationKind
import com.sylo.core.database.notifications.SyloNotification
import com.sylo.core.ui.R
import com.sylo.core.ui.component.SectionLabel
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing
import java.util.Calendar

@Composable
fun NotificationsRoute(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val now = remember(uiState) { System.currentTimeMillis() }
    val today = uiState.notifications.filter { isSameDay(it.timestampEpochMillis, now) }
    val earlier = uiState.notifications.filterNot { isSameDay(it.timestampEpochMillis, now) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SyloSpacing.containerMargin, vertical = SyloSpacing.stackSm),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) }
            Text(
                stringResource(R.string.notif_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            if (uiState.unreadCount > 0) {
                Text(
                    stringResource(R.string.notif_mark_all_read),
                    style = MaterialTheme.typography.labelSmall,
                    color = SyloBrandCyan,
                    // 48dp-tall tap target for accessibility.
                    modifier = Modifier
                        .clickable { viewModel.markAllRead() }
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
        }

        if (!uiState.hasAny) {
            EmptyNotifications()
            return@Column
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm),
        ) {
            if (today.isNotEmpty()) {
                Spacer(Modifier.size(SyloSpacing.stackSm))
                SectionLabel(stringResource(R.string.notif_today))
                today.forEach { NotificationCard(it, now) }
            }
            if (earlier.isNotEmpty()) {
                Spacer(Modifier.size(SyloSpacing.stackSm))
                SectionLabel(stringResource(R.string.notif_earlier))
                earlier.forEach { NotificationCard(it, now) }
            }
            Spacer(Modifier.size(SyloSpacing.stackMd))
            Text(
                stringResource(R.string.notif_clear_all),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { viewModel.clearAll() }.padding(vertical = 16.dp),
            )
            Spacer(Modifier.size(SyloSpacing.stackLg))
        }
    }
}

@Composable
private fun NotificationCard(item: SyloNotification, now: Long) {
    Box {
        SyloCard {
            Row(
                Modifier.padding(SyloSpacing.stackMd),
                horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm),
            ) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Icon(iconFor(item.kind), contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.padding(8.dp).size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(relativeTime(item.timestampEpochMillis, now), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.size(4.dp))
                    Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (!item.read) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 12.dp)
                    .size(width = 4.dp, height = 40.dp)
                    .border(2.dp, SyloBrandCyan, MaterialTheme.shapes.small),
            )
        }
    }
}

@Composable
private fun EmptyNotifications() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(Modifier.size(SyloSpacing.stackMd))
        Text(stringResource(R.string.notif_all_caught_up), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.notif_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun iconFor(kind: NotificationKind): ImageVector = when (kind) {
    NotificationKind.INSIGHT -> Icons.Filled.Insights
    NotificationKind.LARGE_EXPENSE -> Icons.Filled.TrendingUp
    NotificationKind.INCOME -> Icons.Filled.Payments
    NotificationKind.SUMMARY -> Icons.Filled.Summarize
    NotificationKind.CAPTURED -> Icons.Filled.CreditCard
}

@Composable
private fun relativeTime(epochMillis: Long, now: Long): String {
    val diff = (now - epochMillis).coerceAtLeast(0)
    val min = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        min < 1 -> stringResource(R.string.notif_just_now)
        min < 60 -> stringResource(R.string.notif_minutes_ago, min.toInt())
        hours < 24 -> stringResource(R.string.notif_hours_ago, hours.toInt())
        days == 1L -> stringResource(R.string.notif_yesterday)
        else -> stringResource(R.string.notif_days_ago, days.toInt())
    }
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}
