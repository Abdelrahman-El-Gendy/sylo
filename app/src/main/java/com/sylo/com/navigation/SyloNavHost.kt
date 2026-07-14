package com.sylo.com.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.sylo.com.notifications.NotificationsRoute as NotificationsScreen
import com.sylo.core.navigation.AnalyticsRoute
import com.sylo.core.navigation.DashboardRoute
import com.sylo.core.navigation.LoginRoute
import com.sylo.core.navigation.NotificationsRoute
import com.sylo.core.navigation.OnboardingRoute
import com.sylo.core.navigation.PinSetupRoute
import com.sylo.core.navigation.SettingsRoute
import com.sylo.core.navigation.SyloDestination
import com.sylo.core.navigation.TransactionDetailRoute
import com.sylo.core.navigation.TransactionsRoute
import com.sylo.core.navigation.VoiceCaptureRoute
import com.sylo.core.navigation.VoiceReviewRoute
import com.sylo.core.ui.component.LocalOnNotificationsClick
import com.sylo.feature.auth.navigation.authEntries
import com.sylo.feature.auth.navigation.pinSetupEntry
import com.sylo.feature.dashboard.navigation.analyticsEntry
import com.sylo.feature.dashboard.navigation.dashboardEntry
import com.sylo.feature.settings.BalanceSetupRoute
import com.sylo.feature.settings.navigation.settingsEntry
import com.sylo.feature.transactions.AddExpenseSheet
import com.sylo.feature.transactions.navigation.historyEntry
import com.sylo.feature.transactions.navigation.transactionDetailEntry
import com.sylo.feature.voice.navigation.voiceEntries

private data class TabItem(val route: SyloDestination, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(DashboardRoute, "Home", Icons.Filled.Home),
    TabItem(TransactionsRoute, "History", Icons.AutoMirrored.Filled.ReceiptLong),
    TabItem(AnalyticsRoute, "Analytics", Icons.Filled.InsertChart),
    TabItem(SettingsRoute, "Settings", Icons.Filled.Settings),
)

private val TOP_LEVEL_ROUTES: Set<NavKey> = tabs.map { it.route }.toSet()

/**
 * The app root, gated in three stages:
 *
 * 1. **Auth** — while unauthenticated we show the auth flow (onboarding → PIN).
 * 2. **First-run balance** — once authenticated, a brand-new user who has never set
 *    an opening balance is sent to a mandatory balance-setup screen.
 * 3. **Main** — the tabbed experience.
 *
 * This replaces the Navigation 2 `AuthGraph -> MainGraph` nested graphs and the
 * `popUpTo(AuthGraph){ inclusive }` transition.
 *
 * Authentication is intentionally **session-only** (`remember`, not `rememberSaveable`):
 * a cold start or process death re-locks the app, so the PIN is required every launch.
 */
@Composable
fun SyloNavHost(modifier: Modifier = Modifier) {
    var isAuthenticated by remember { mutableStateOf(false) }

    val appViewModel: SyloAppViewModel = hiltViewModel()
    val balanceConfigured by appViewModel.balanceConfigured.collectAsStateWithLifecycle()

    when {
        !isAuthenticated -> AuthFlow(
            modifier = modifier,
            onAuthenticated = { isAuthenticated = true },
        )

        // null = the persisted flag is still loading; avoid flashing the wrong branch.
        balanceConfigured == null -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        balanceConfigured == false -> Box(modifier = modifier.fillMaxSize()) {
            // On save, the persisted flag flips to true and this gate recomposes to MainFlow.
            BalanceSetupRoute(onSaved = {})
        }

        else -> MainFlow(
            modifier = modifier,
            onLoggedOut = { isAuthenticated = false },
        )
    }
}

/**
 * The pre-auth flow: onboarding -> PIN unlock -> (first-run) PIN setup. A single
 * back stack; success is signalled up via [onAuthenticated].
 */
@Composable
private fun AuthFlow(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(OnboardingRoute)

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            authEntries(
                onAuthenticated = onAuthenticated,
                onNeedSetup = { backStack.add(PinSetupRoute) },
                onOnboardingFinished = {
                    // Onboarding is one-shot: replace it with the login screen so
                    // Back doesn't return to the carousel.
                    backStack.clear()
                    backStack.add(LoginRoute)
                },
                onPinSetupClose = { backStack.removeLastOrNull() },
            )
        },
    )
}

/**
 * The main, post-auth experience: a Scaffold whose bottom bar (Home · History ·
 * Voice · Analytics · Settings) appears only on the bare tab screens. Each tab owns
 * its own back stack via [NavigationState]; features contribute their destinations
 * as entries and never reference each other, only :core-navigation route contracts.
 */
@Composable
private fun MainFlow(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigationState = rememberNavigationState(
        startRoute = DashboardRoute,
        topLevelRoutes = TOP_LEVEL_ROUTES,
    )
    val navigator = remember { Navigator(navigationState) }

    // Add-expense is a modal bottom sheet over whatever screen is showing, not a route.
    var showAddExpense by rememberSaveable { mutableStateOf(false) }

    // Bottom bar shows only when the active tab's stack is at its root (no child pushed).
    val activeStack = navigationState.backStacks[navigationState.topLevelRoute]
    val showBars = activeStack?.lastOrNull() == navigationState.topLevelRoute

    val entryProvider = entryProvider {
        dashboardEntry(
            onTransactionClick = { id -> navigator.navigate(TransactionDetailRoute(id)) },
            onSeeAllClick = { navigator.navigate(TransactionsRoute) },
            onAddExpense = { showAddExpense = true },
        )
        historyEntry(
            onTransactionClick = { id -> navigator.navigate(TransactionDetailRoute(id)) },
        )
        analyticsEntry()
        settingsEntry(
            onChangePin = { navigator.navigate(PinSetupRoute) },
            onLoggedOut = onLoggedOut,
        )

        // Full-screen routes (no bottom bar)
        transactionDetailEntry(
            onBack = { navigator.goBack() },
            onEdit = { showAddExpense = true },
        )
        voiceEntries(
            onSaved = { navigator.exitFlowToHome(DashboardRoute) },
            onEdit = { transcript -> navigator.navigate(VoiceReviewRoute(transcript)) },
            onConfirmSave = { navigator.exitFlowToHome(DashboardRoute) },
            onEditDetails = { showAddExpense = true },
        )
        entry<NotificationsRoute> {
            NotificationsScreen(onBack = { navigator.goBack() })
        }
        // Re-enrolling a PIN from Settings reuses the auth feature's screen; here it
        // just returns to Settings when done.
        pinSetupEntry(
            onPinCreated = { navigator.goBack() },
            onClose = { navigator.goBack() },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBars) {
                SyloBottomBar(
                    navigationState = navigationState,
                    onTabClick = { route -> navigator.navigate(route) },
                    onVoiceClick = { navigator.navigate(VoiceCaptureRoute) },
                )
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalOnNotificationsClick provides { navigator.navigate(NotificationsRoute) },
        ) {
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = { navigator.goBack() },
                modifier = Modifier.padding(innerPadding),
            )

            if (showAddExpense) {
                AddExpenseSheet(
                    onDismiss = { showAddExpense = false },
                    onSaved = { showAddExpense = false },
                )
            }
        }
    }
}

@Composable
private fun SyloBottomBar(
    navigationState: NavigationState,
    onTabClick: (SyloDestination) -> Unit,
    onVoiceClick: () -> Unit,
) {
    val tabColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primaryContainer,
        indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            tabs.forEachIndexed { index, tab ->
                // Reserve the middle slot for the docked mic FAB.
                if (index == 2) {
                    NavigationBarItem(selected = false, enabled = false, onClick = {}, icon = {}, label = null)
                }
                NavigationBarItem(
                    selected = tab.route == navigationState.topLevelRoute,
                    onClick = { onTabClick(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    colors = tabColors,
                )
            }
        }
        // Center mic FAB docked onto the bar's top edge (straddling it, not floating far above).
        FloatingActionButton(
            onClick = onVoiceClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp),
        ) {
            Icon(Icons.Filled.Mic, contentDescription = "Voice capture")
        }
    }
}
