package com.sylo.com.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events for the main tabbed area by updating [NavigationState].
 *
 * Follows Unidirectional Data Flow: the `Navigator` mutates the state, and
 * `NavDisplay` observes the state and re-renders.
 */
class Navigator(val state: NavigationState) {

    /**
     * Navigate to [route]. A top-level route (a tab) switches the active stack; any
     * other route is pushed onto the current stack.
     */
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    /** Pop the current stack; if already at the tab root, fall back to home. */
    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    /**
     * Clear every non-root entry from the current stack and switch to [homeRoute].
     *
     * Used by the voice flow (intro → capture → review): after saving or cancelling
     * we drop the whole flow and return the user to Home, matching the old
     * `popBackStack(DashboardRoute, inclusive = false)` behavior.
     */
    fun exitFlowToHome(homeRoute: NavKey) {
        val currentStack = state.backStacks[state.topLevelRoute]
        if (currentStack != null) {
            while (currentStack.size > 1) {
                currentStack.removeAt(currentStack.size - 1)
            }
        }
        state.topLevelRoute = homeRoute
    }
}
