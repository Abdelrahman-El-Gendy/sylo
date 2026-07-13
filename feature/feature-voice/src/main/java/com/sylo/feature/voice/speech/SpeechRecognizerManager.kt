package com.sylo.feature.voice.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Android's on-device [SpeechRecognizer]. Exposes recognition
 * progress as a [StateFlow] the UI/ViewModel can observe.
 *
 * SpeechRecognizer must be created and driven on the MAIN thread, so every public
 * method here is expected to be called from the main dispatcher (ViewModel does so).
 */
@Singleton
class SpeechRecognizerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class State(
        val available: Boolean = true,
        val isListening: Boolean = false,
        val partialText: String = "",
        val finalText: String? = null,
        val rms: Float = 0f,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State(available = SpeechRecognizer.isRecognitionAvailable(context)))
    val state: StateFlow<State> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun startListening(languageTag: String = Locale.getDefault().toLanguageTag()) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update {
                it.copy(available = false, isListening = false, error = "Speech recognition isn't available on this device.")
            }
            return
        }

        val sr = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        _state.update {
            it.copy(available = true, isListening = true, error = null, finalText = null, partialText = "")
        }
        sr.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        _state.update { it.copy(isListening = false) }
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
        _state.update { it.copy(isListening = false) }
    }

    private fun firstResult(bundle: Bundle?): String =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = _state.update { it.copy(isListening = true) }
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = _state.update { it.copy(rms = rmsdB) }
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = _state.update { it.copy(isListening = false) }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults)
            if (text.isNotBlank()) _state.update { it.copy(partialText = text) }
        }

        override fun onResults(results: Bundle?) {
            val text = firstResult(results)
            _state.update {
                it.copy(isListening = false, finalText = text, partialText = text.ifBlank { it.partialText })
            }
        }

        override fun onError(error: Int) {
            _state.update { it.copy(isListening = false, error = errorMessage(error)) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Recognition client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Recognition error ($code)"
    }
}
