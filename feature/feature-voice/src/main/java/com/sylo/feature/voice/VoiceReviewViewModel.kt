package com.sylo.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.feature.voice.speech.ParsedExpense
import com.sylo.feature.voice.speech.TranscriptParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class VoiceReviewUiState(
    val parsed: ParsedExpense,
    /** The account's display currency — what the expense is actually saved in. */
    val currency: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
) {
    /** Only a positive amount can be saved; "hello" with no number must not persist. */
    val canSave: Boolean get() = parsed.amountMinor > 0L
}

/**
 * Parses the recognized transcript into an expense and — on confirm — persists it
 * to the SQLCipher-encrypted Room database via [TransactionRepository].
 *
 * The expense is always stored in the user's account currency (the app has no FX
 * conversion), keeping the dashboard balance math consistent with manual entries.
 */
@HiltViewModel
class VoiceReviewViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val userPreferences: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceReviewUiState(parsed = TranscriptParser.parse("")))
    val uiState: StateFlow<VoiceReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val currency = userPreferences.currency.first()
            _uiState.update { it.copy(currency = currency) }
        }
    }

    fun setTranscript(text: String) {
        _uiState.update { it.copy(parsed = TranscriptParser.parse(text)) }
    }

    /** Persist the parsed expense locally, then invoke [onSaved]. No-op if nothing to save. */
    fun confirmAndSave(onSaved: () -> Unit) {
        val current = _uiState.value
        if (current.isSaving || !current.canSave) return
        val parsed = current.parsed
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val currency = current.currency.ifBlank { userPreferences.currency.first() }
            repository.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    title = parsed.note.ifBlank { "Voice expense" },
                    amountMinor = -parsed.amountMinor, // spoken expenses are outflows
                    currency = currency,
                    category = parsed.category,
                    note = parsed.note,
                    status = "Approved",
                    timestampEpochMillis = System.currentTimeMillis(),
                )
            )
            _uiState.update { it.copy(isSaving = false, saved = true) }
            onSaved()
        }
    }
}
