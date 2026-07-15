package com.sylo.feature.voice.speech

import android.content.Context
import android.content.Intent
import android.os.Build
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
 * Thin wrapper around Android's [SpeechRecognizer]. Exposes recognition
 * progress as a [StateFlow] the UI/ViewModel can observe.
 *
 * Language handling: the platform's automatic language identification
 * (EXTRA_ENABLE_LANGUAGE_DETECTION / EXTRA_ENABLE_LANGUAGE_SWITCH) is honored only
 * by the ON-DEVICE recognizer on Android 14+, so when that engine is available we
 * use it with detection + mid-session switching enabled and no language pinned —
 * the recognizer identifies whatever language is spoken (Arabic, German, …) and
 * switches to it. On devices without on-device recognition (or below Android 14)
 * the platform offers no auto-detection at all, so we fall back to the default
 * recognizer pinned to the device locale.
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
        /** BCP-47 tag of the language the recognizer identified, when supported. */
        val detectedLanguage: String? = null,
    )

    private val _state = MutableStateFlow(State(available = SpeechRecognizer.isRecognitionAvailable(context)))
    val state: StateFlow<State> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    /** True when the API 34+ on-device engine (the one that supports detection) is in use. */
    private var usingOnDeviceEngine = false

    /**
     * Set when the on-device engine turned out to have no usable language model
     * (ERROR_LANGUAGE_UNAVAILABLE / NOT_SUPPORTED) — from then on we stick to the
     * default service, which at least recognizes the device locale.
     */
    private var onDeviceEngineUnusable = false

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update {
                it.copy(available = false, isListening = false, error = "Speech recognition isn't available on this device.")
            }
            return
        }

        val sr = recognizer ?: createBestRecognizer().also {
            it.setRecognitionListener(listener)
            recognizer = it
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (usingOnDeviceEngine && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Do NOT pin EXTRA_LANGUAGE here — that would bias every utterance
                // toward one language. Let the engine identify the spoken language
                // (unconstrained) and switch to it mid-session.
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putExtra(
                    RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                    RecognizerIntent.LANGUAGE_SWITCH_BALANCED,
                )
            } else {
                // No platform auto-detection available: best effort is the device locale.
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            }
        }
        _state.update {
            it.copy(
                available = true, isListening = true, error = null,
                finalText = null, partialText = "", detectedLanguage = null,
            )
        }
        sr.startListening(intent)
    }

    /**
     * Prefers the on-device engine on Android 14+ because it is the only one that
     * honors the language-detection/switch extras; otherwise the default service.
     */
    private fun createBestRecognizer(): SpeechRecognizer =
        if (!onDeviceEngineUnusable &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            usingOnDeviceEngine = true
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            usingOnDeviceEngine = false
            SpeechRecognizer.createSpeechRecognizer(context)
        }

    fun stopListening() {
        recognizer?.stopListening()
        _state.update { it.copy(isListening = false) }
    }

    /** Clears the last result so the UI can return to the idle "hold to speak" state. */
    fun reset() {
        _state.update {
            it.copy(
                isListening = false, partialText = "", finalText = null,
                error = null, rms = 0f, detectedLanguage = null,
            )
        }
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

        // Fired by the on-device engine (API 34+) whenever it identifies the spoken
        // language — surfaced so the UI can show what was detected.
        override fun onLanguageDetection(results: Bundle) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                results.getString(SpeechRecognizer.DETECTED_LANGUAGE)?.let { tag ->
                    _state.update { it.copy(detectedLanguage = tag) }
                }
            }
        }

        override fun onError(error: Int) {
            val missingModel = error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
                error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
            if (missingModel && usingOnDeviceEngine) {
                // The on-device engine exists but has no language model installed.
                // Fall back to the default service permanently; if the user is still
                // holding the mic, restart the session on it seamlessly.
                onDeviceEngineUnusable = true
                recognizer?.destroy()
                recognizer = null
                usingOnDeviceEngine = false
                if (_state.value.isListening) {
                    startListening()
                    return
                }
            }
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
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "That language pack isn't installed on this device"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language model unavailable — check speech language packs"
        else -> "Recognition error ($code)"
    }
}
