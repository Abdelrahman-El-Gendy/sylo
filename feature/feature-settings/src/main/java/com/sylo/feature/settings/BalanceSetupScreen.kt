package com.sylo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.sylo.core.ui.component.SyloAmountField
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.component.SyloPrimaryButton
import com.sylo.core.ui.component.currencySymbol
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing

/**
 * Mandatory first-run step: a full screen that asks the user to set their opening
 * balance before entering the app. Reuses [BalanceSetupViewModel]; [onSaved] fires
 * once the balance is persisted (and the `balanceConfigured` flag is set).
 */
@Composable
fun BalanceSetupRoute(
    onSaved: () -> Unit,
    viewModel: BalanceSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = SyloSpacing.containerMargin)
            .padding(top = SyloSpacing.sectionGap, bottom = SyloSpacing.stackLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Brand badge
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(72.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = SyloBrandCyan,
                modifier = Modifier.padding(18.dp),
            )
        }

        Spacer(Modifier.height(SyloSpacing.stackLg))
        Text(
            "Set your opening balance",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SyloSpacing.stackSm))
        Text(
            "This is the money you have right now. Your balance updates automatically as you add expenses.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(SyloSpacing.stackLg))

        // Hero amount card
        SyloCard {
            Column(
                modifier = Modifier.padding(
                    horizontal = SyloSpacing.stackMd,
                    vertical = SyloSpacing.stackLg,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SectionLabel("Current balance")
                Spacer(Modifier.height(SyloSpacing.stackMd))
                SyloAmountField(
                    amount = uiState.amount,
                    onAmountChange = viewModel::onAmountChange,
                    currencySymbol = currencySymbol(uiState.currency),
                )
            }
        }

        Spacer(Modifier.height(SyloSpacing.stackLg))
        SectionLabel("Currency", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(SyloSpacing.stackSm))
        CurrencyGrid(
            currencies = viewModel.currencies,
            selected = uiState.currency,
            onSelect = viewModel::onCurrencyChange,
        )

        Spacer(Modifier.weight(1f, fill = true))

        SyloPrimaryButton(
            text = "Continue",
            onClick = { viewModel.save(onSaved) },
            enabled = uiState.isValid,
        )
        Spacer(Modifier.height(SyloSpacing.stackMd))
        TrustFooter()
    }
}

/** Bottom sheet for editing the opening balance + display currency (from Settings). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceCurrencySheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: BalanceSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SyloSpacing.containerMargin)
                .padding(bottom = SyloSpacing.stackMd)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Balance & Currency", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(SyloSpacing.stackMd))
            SectionLabel("Current balance")
            Spacer(Modifier.height(SyloSpacing.stackSm))
            SyloAmountField(
                amount = uiState.amount,
                onAmountChange = viewModel::onAmountChange,
                currencySymbol = currencySymbol(uiState.currency),
            )

            Spacer(Modifier.height(SyloSpacing.stackMd))
            SectionLabel("Currency", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(SyloSpacing.stackSm))
            CurrencyGrid(
                currencies = viewModel.currencies,
                selected = uiState.currency,
                onSelect = viewModel::onCurrencyChange,
            )

            Spacer(Modifier.height(SyloSpacing.stackLg))
            SyloPrimaryButton(text = "Save", onClick = { viewModel.save(onSaved) }, enabled = uiState.isValid)
        }
    }
}

/** A compact, non-scrolling 4-column grid of currency chips (safe inside a scroll). */
@Composable
private fun CurrencyGrid(
    currencies: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
        currencies.chunked(4).forEach { rowCodes ->
            Row(horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
                rowCodes.forEach { code ->
                    CurrencyChip(
                        code = code,
                        selected = code == selected,
                        onClick = { onSelect(code) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep partial rows aligned to the 4-column grid.
                repeat(4 - rowCodes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CurrencyChip(
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) SyloBrandCyan.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) SyloBrandCyan else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .then(if (selected) Modifier.border(1.dp, SyloBrandCyan, MaterialTheme.shapes.medium) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(code, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = content)
    }
}

/** Reassurance line shown under the first-run CTA. */
@Composable
private fun TrustFooter() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(SyloSpacing.unit))
        Text(
            "You can change this anytime in Settings",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

