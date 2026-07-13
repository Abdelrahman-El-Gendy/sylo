package com.sylo.com.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * Holds the navigation state for the main (post-auth) tabbed area of the app.
 *
 * Each top-level route (a bottom-nav tab) owns its own back stack, and switching
 * tabs retains the state of every stack. This replaces the Navigation 2
 * `popUpTo(startDestination){ saveState } + restoreState` dance we used to write by
 * hand on the `NavController`.
 *
 * The current top-level route and each back stack survive configuration changes and
 * process death via `rememberSerializable`/`rememberNavBackStack`.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): NavigationState {

    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * State holder for navigation state. It does not modify its own state — use
 * [Navigator] for that.
 *
 * @param startRoute the start route; the user exits the app through this route.
 * @param topLevelRoute the current top-level route.
 * @param backStacks the back stack for each top-level route.
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    /**
     * Top-level routes currently in use. The start route is always first, so the
     * user always exits through home ("exit through home" pattern). At most one
     * other route follows it; state for inactive routes is still retained.
     */
    private val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    /**
     * Convert the navigation state into `NavEntry`s decorated with a
     * `SaveableStateHolder` (one per back stack, so each tab keeps its own state).
     */
    @Composable
    fun toEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
    ): SnapshotStateList<NavEntry<NavKey>> {

        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            // SaveableStateHolder keeps each entry's rememberSaveable state; the
            // ViewModelStore decorator scopes hiltViewModel() to the NavEntry so a
            // route like TransactionDetailRoute(id) gets its own ViewModel instance.
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator(),
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }

        return stacksInUse
            .flatMap { decoratedEntries[it] ?: emptyList() }
            .toMutableStateList()
    }
}
