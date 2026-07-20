package com.sylo.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.ui.R
import com.sylo.core.ui.component.SectionLabel
import com.sylo.core.ui.component.SyloTopBar
import com.sylo.core.ui.component.TransactionRow
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing

@Composable
fun HistoryRoute(
    onTransactionClick: (String) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onTransactionClick = onTransactionClick,
    )
}

@Composable
private fun HistoryScreen(
    uiState: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (HistoryFilter) -> Unit,
    onTransactionClick: (String) -> Unit,
) {
    // Adaptive: one column on phones, more on tablets/foldables as width allows.
    // Header rows and section labels span the full width; transactions are the cells.
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
        val fullSpan: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan =
            { GridItemSpan(maxLineSpan) }

        item(span = fullSpan) { SyloTopBar() }

        item(span = fullSpan) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = SyloBrandCyan,
                ),
            )
        }

        item(span = fullSpan) {
            Row(horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
                HistoryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { onFilterChange(filter) },
                        label = { Text(stringResource(filter.labelRes)) },
                        shape = MaterialTheme.shapes.large,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SyloBrandCyan,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        if (uiState.groups.isEmpty()) {
            item(span = fullSpan) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(if (uiState.query.isBlank()) R.string.history_empty_none else R.string.history_empty_search),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(if (uiState.query.isBlank()) R.string.history_empty_none_hint else R.string.history_empty_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        uiState.groups.forEach { group ->
            item(span = fullSpan, key = "header-${group.label}") {
                SectionLabel(group.label)
            }
            items(group.items, key = { it.id }) { tx ->
                TransactionRow(
                    transaction = tx,
                    emphasizeExpense = true,
                    onClick = { onTransactionClick(tx.id) },
                )
            }
        }
    }
}
