package com.sylo.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sylo.core.ui.theme.MonoNumberStyle
import com.sylo.core.ui.theme.SyloIncomeGreen

/** Presentation model for a transaction, shared by dashboard + history (features can't depend on each other). */
data class UiTransaction(
    val id: String,
    val merchant: String,
    val subtitle: String,
    val amountLabel: String,
    val icon: ImageVector,
    val isIncome: Boolean = false,
    val status: String? = null,
)

/**
 * A single transaction row: category icon tile, merchant + subtitle, and a
 * monospaced amount (green for income, red for expense when [emphasizeExpense]).
 */
@Composable
fun TransactionRow(
    transaction: UiTransaction,
    modifier: Modifier = Modifier,
    emphasizeExpense: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val amountColor = when {
        transaction.isIncome -> SyloIncomeGreen
        emphasizeExpense -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    SyloCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Icon(
                    imageVector = transaction.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp).size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    transaction.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(transaction.amountLabel, style = MonoNumberStyle, color = amountColor)
                transaction.status?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
