package com.sylo.feature.transactions.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sylo.core.navigation.TransactionDetailRoute
import com.sylo.core.navigation.TransactionsRoute
import com.sylo.feature.transactions.HistoryRoute
import com.sylo.feature.transactions.TransactionDetailRoute as TransactionDetailScreenRoute

/** The "History" tab. */
fun EntryProviderScope<NavKey>.historyEntry(
    onTransactionClick: (String) -> Unit,
) {
    entry<TransactionsRoute> {
        HistoryRoute(onTransactionClick = onTransactionClick)
    }
}

/** Full-screen transaction detail. The id arrives on the route key. */
fun EntryProviderScope<NavKey>.transactionDetailEntry(
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    entry<TransactionDetailRoute> { key ->
        TransactionDetailScreenRoute(
            transactionId = key.id,
            onBack = onBack,
            onEdit = onEdit,
        )
    }
}
