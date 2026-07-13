package com.sylo.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.feature.voice.speech.SpeechRecognizerManager
import com.sylo.feature.voice.speech.TranscriptParser
import com.sylo.feature.voice.speech.VoiceLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VoiceCaptureViewModel @Inject constructor(
    private val recognizer: SpeechRecognizerManager,
    repository: TransactionRepository,
    userPreferences: UserPreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SpeechRecognizerManager.State> = recognizer.state

    /** Past transactions, used to surface a spending-context hint for the spoken category. */
    val transactions: StateFlow<List<TransactionEntity>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currency: StateFlow<String> = userPreferences.currency
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferencesRepository.DEFAULT_CURRENCY)

    /** Dictation language, defaulting to the device locale; user can override per-recording. */
    private val _language = MutableStateFlow(VoiceLanguage.systemDefault())
    val language: StateFlow<VoiceLanguage> = _language.asStateFlow()

    fun setLanguage(language: VoiceLanguage) {
        _language.value = language
    }

    /** Begins capturing while the mic is held, in the currently selected [VoiceLanguage]. */
    fun startListening() = recognizer.startListening(_language.value.languageTag)

    /** Stops capturing on release; the recognizer then delivers its final result. */
    fun stopListening() = recognizer.stopListening()

    /** The best text we have so far (final result if present, otherwise the live partial). */
    fun currentTranscript(): String {
        val s = recognizer.state.value
        return s.finalText?.takeIf { it.isNotBlank() } ?: s.partialText
    }

    /** Live guess of the category, shown under the transcript. */
    fun currentCategory(): String = TranscriptParser.parse(currentTranscript()).category

    /** Live guess of the amount, shown as a preview. */
    fun currentAmount(): String = TranscriptParser.parse(currentTranscript()).amountDisplay

    override fun onCleared() {
        recognizer.destroy()
    }
}
