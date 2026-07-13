package com.sylo.com.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.notifications.NotificationsRepository
import com.sylo.core.database.notifications.SyloNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<SyloNotification> = emptyList(),
) {
    val hasAny: Boolean get() = notifications.isNotEmpty()
    val unreadCount: Int get() = notifications.count { !it.read }
}

/** Notifications are derived from real transactions by [NotificationsRepository]. */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository,
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = repository.observe()
        .map { NotificationsUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    fun markAllRead() = viewModelScope.launch {
        repository.markRead(uiState.value.notifications.map { it.id }.toSet())
    }

    fun clearAll() = viewModelScope.launch {
        repository.clear(uiState.value.notifications.map { it.id }.toSet())
    }
}
