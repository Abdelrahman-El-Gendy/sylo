package com.sylo.feature.auth.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sylo.core.navigation.LoginRoute
import com.sylo.core.navigation.OnboardingRoute
import com.sylo.core.navigation.PinSetupRoute
import com.sylo.feature.auth.ui.OnboardingRoute as OnboardingScreenRoute
import com.sylo.feature.auth.ui.PinSetupRoute as PinSetupScreenRoute
import com.sylo.feature.auth.ui.PinUnlockRoute

/**
 * The auth feature contributes its destinations as entries on an [EntryProviderScope].
 * :app assembles these into the auth-flow `NavDisplay` — the only coupling point, and
 * it flows through :core-navigation route contracts.
 *
 * The onboarding → login → (optional) PIN-setup transitions are driven by :app's
 * auth back stack; successful unlock/enrollment is signalled through [onAuthenticated].
 */
fun EntryProviderScope<NavKey>.authEntries(
    onAuthenticated: () -> Unit,
    onNeedSetup: () -> Unit,
    onOnboardingFinished: () -> Unit,
    onPinSetupClose: () -> Unit,
) {
    entry<OnboardingRoute> {
        OnboardingScreenRoute(onFinished = onOnboardingFinished)
    }
    entry<LoginRoute> {
        PinUnlockRoute(onUnlocked = onAuthenticated, onNeedSetup = onNeedSetup)
    }
    pinSetupEntry(onPinCreated = onAuthenticated, onClose = onPinSetupClose)
}

/**
 * The PIN setup/enrollment screen as a standalone entry. Reused by both the auth
 * flow (first-run enrollment) and the main flow (Settings → "change PIN"), each
 * wiring its own completion callbacks.
 */
fun EntryProviderScope<NavKey>.pinSetupEntry(
    onPinCreated: () -> Unit,
    onClose: () -> Unit,
) {
    entry<PinSetupRoute> {
        PinSetupScreenRoute(onPinCreated = onPinCreated, onClose = onClose)
    }
}
