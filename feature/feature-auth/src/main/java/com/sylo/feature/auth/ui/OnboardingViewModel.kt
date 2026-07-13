package com.sylo.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.security.pin.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the onboarding route should do once the persisted state has loaded. */
enum class OnboardingState { Loading, Show, Skip }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferencesRepository,
    private val pinManager: PinManager,
) : ViewModel() {

    /**
     * Onboarding is shown only to a brand-new user: one who has NOT registered
     * (no PIN yet) AND has never completed onboarding. Anyone with a PIN is a
     * returning, registered user and is skipped straight through — even if the
     * completion flag is somehow unset (fresh install over old data, cleared
     * DataStore, an update that introduced onboarding, etc.).
     */
    val state: StateFlow<OnboardingState> = userPreferences.onboardingCompleted
        .map { completed ->
            if (completed || pinManager.hasPin()) OnboardingState.Skip else OnboardingState.Show
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingState.Loading)

    /** Persist completion so onboarding never shows again; state then flips to [OnboardingState.Skip]. */
    fun complete() {
        viewModelScope.launch { userPreferences.setOnboardingCompleted(true) }
    }
}
