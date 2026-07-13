package com.sylo.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.ui.component.SectionLabel
import com.sylo.core.ui.component.Sparkline
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.component.SyloTopBar
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloIncomeGreen
import com.sylo.core.ui.theme.SyloSpacing

@Composable
fun AnalyticsRoute(
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsScreen(uiState, viewModel::onPeriodChange)
}

@Composable
private fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
) {
    // Adaptive: the header, chart card, and section label span full width; the
    // category-breakdown rows flow into more columns on tablets/foldables.
    val fullSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SyloSpacing.containerMargin,
            end = SyloSpacing.containerMargin,
            bottom = SyloSpacing.sectionGap,
        ),
        horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm),
        verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackMd),
    ) {
        item(span = fullSpan) { SyloTopBar() }
        item(span = fullSpan) {
            Text("Analytics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item(span = fullSpan) { PeriodTabs(selected = uiState.period, onSelect = onPeriodChange) }

        if (!uiState.hasData) {
            item(span = fullSpan) { EmptyAnalytics() }
            return@LazyVerticalGrid
        }

        // Total spending + trend + chart (full width — the chart benefits from it)
        item(span = fullSpan) {
            SyloCard {
                Column(Modifier.padding(SyloSpacing.stackMd)) {
                    Text("Total Spending", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.totalSpending, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(SyloSpacing.stackSm))
                        uiState.trendPercent?.let { TrendPill(it, uiState.trendDown) }
                    }
                    Spacer(Modifier.height(SyloSpacing.stackSm))
                    Sparkline(points = uiState.trend, modifier = Modifier.fillMaxWidth().height(100.dp))
                }
            }
        }

        item(span = fullSpan) { SectionLabel("Category Breakdown") }
        items(uiState.categories, key = { it.category }) { CategoryRow(it) }
    }
}

@Composable
private fun PeriodTabs(selected: AnalyticsPeriod, onSelect: (AnalyticsPeriod) -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            AnalyticsPeriod.entries.forEach { period ->
                val isSelected = period == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelect(period) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        period.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendPill(text: String, down: Boolean) {
    val color = if (down) SyloIncomeGreen else MaterialTheme.colorScheme.error
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (down) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null, tint = color, modifier = Modifier.size(16.dp),
            )
            Text(text, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun CategoryRow(item: CategorySpend) {
    SyloCard {
        Row(
            Modifier.padding(SyloSpacing.stackMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Icon(item.icon, contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.padding(8.dp).size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(item.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("${item.transactions} transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.amountLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("${item.percent}%", style = MaterialTheme.typography.labelSmall, color = SyloBrandCyan)
            }
        }
    }
}

@Composable
private fun EmptyAnalytics() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.InsertChart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(SyloSpacing.stackMd))
        Text("No spending yet", style = MaterialTheme.typography.titleLarge)
        Text(
            "Add expenses to see your spending analytics.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
