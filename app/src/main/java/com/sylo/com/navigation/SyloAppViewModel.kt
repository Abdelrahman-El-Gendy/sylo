package com.sylo.com.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Root-level app state that drives the top-level gate in [SyloNavHost].
 *
 * [balanceConfigured] is `null` while the persisted value is still loading, so the
 * gate can avoid flashing the wrong branch on cold start.
 */
@HiltViewModel
class SyloAppViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
) : ViewModel() {

    val balanceConfigured: StateFlow<Boolean?> = preferences.balanceConfigured
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
