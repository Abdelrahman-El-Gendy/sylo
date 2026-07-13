package com.sylo.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation route contracts shared across the whole app.
 *
 * This is the ONLY thing features share about navigation: a feature declares where
 * it can send the user by referencing these types, never by depending on another
 * feature module. :app owns the NavDisplay and maps each route to a feature's screen.
 *
 * Every route is a Navigation 3 [NavKey], so it can live directly in a back stack.
 * There are no "graph" keys: top-level routes (the bottom-nav tabs) identify each
 * back stack, and the auth flow is gated at the root rather than by a nested graph.
 */
sealed interface SyloDestination : NavKey

// ----- Auth -----
/** First-run onboarding carousel, shown before the PIN flow. */
@Serializable
data object OnboardingRoute : SyloDestination

@Serializable
data object PinSetupRoute : SyloDestination

@Serializable
data object LoginRoute : SyloDestination

// ----- Bottom-nav tabs -----
@Serializable
data object DashboardRoute : SyloDestination

/** Transaction history (the "History" tab). */
@Serializable
data object TransactionsRoute : SyloDestination

@Serializable
data object AnalyticsRoute : SyloDestination

@Serializable
data object SettingsRoute : SyloDestination

/** Notifications list, opened from the bell in the top bar. */
@Serializable
data object NotificationsRoute : SyloDestination

// ----- Transaction detail / entry (full-screen, above the bottom bar) -----
@Serializable
data class TransactionDetailRoute(val id: String) : SyloDestination

@Serializable
data object AddExpenseRoute : SyloDestination

// ----- Voice flow: capture (hold-to-talk) -> review -----
@Serializable
data object VoiceCaptureRoute : SyloDestination

/** Carries the recognized speech text forward to the review/confirm step. */
@Serializable
data class VoiceReviewRoute(val transcript: String) : SyloDestination
