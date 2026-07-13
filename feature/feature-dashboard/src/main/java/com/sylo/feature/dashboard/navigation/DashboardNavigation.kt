package com.sylo.feature.dashboard.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sylo.core.navigation.AnalyticsRoute
import com.sylo.core.navigation.DashboardRoute
import com.sylo.feature.dashboard.AnalyticsRoute as AnalyticsScreenRoute
import com.sylo.feature.dashboard.DashboardRoute as DashboardScreenRoute

fun EntryProviderScope<NavKey>.dashboardEntry(
    onTransactionClick: (String) -> Unit,
    onSeeAllClick: () -> Unit,
    onAddExpense: () -> Unit,
) {
    entry<DashboardRoute> {
        DashboardScreenRoute(
            onTransactionClick = onTransactionClick,
            onSeeAllClick = onSeeAllClick,
            onAddExpense = onAddExpense,
        )
    }
}

fun EntryProviderScope<NavKey>.analyticsEntry() {
    entry<AnalyticsRoute> { AnalyticsScreenRoute() }
}
