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
 * Dictation language for voice capture. [AUTO] asks the platform to identify the
 * spoken language; the rest pin a specific one. A specific choice is the reliable
 * escape hatch when auto-detection isn't supported on the device (see
 * [SpeechRecognizerManager]).
 */
enum class VoiceLanguage(val tag: String?, val label: String) {
    AUTO(null, "Auto"),
    ENGLISH("en-US", "EN"),
    ARABIC("ar-SA", "AR"),
    FRENCH("fr-FR", "FR"),
    GERMAN("de-DE", "DE"),
    SPANISH("es-ES", "ES"),
    ;

    companion object {
        fun fromKeyOrDefault(key: String?): VoiceLanguage =
            entries.firstOrNull { it.name == key } ?: AUTO
    }
}

/**
 * Thin wrapper around Android's [SpeechRecognizer]. Exposes recognition progress as
 * a [StateFlow] the UI/ViewModel can observe.
 *
 * Language handling — the platform gives us two engines with very different abilities:
 *
 * - **[VoiceLanguage.AUTO]**: real automatic language identification exists only on
 *   the ON-DEVICE recognizer (Android 14+), so we use it with detection + mid-session
 *   switching and no language pinned. It's frequently unavailable or only has the
 *   English pack installed, which is why a specific language is offered as a fallback.
 * - **A specific [VoiceLanguage]**: we use the DEFAULT (Google) recognizer pinned to
 *   that BCP-47 tag. This is the dependable path — Google's recognizer transcribes
 *   Arabic, German, French, … accurately when told which one to expect.
 *
 * The active engine is swapped automatically when the requested mode needs the other
 * one. SpeechRecognizer must be created and driven on the MAIN thread.
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
        /** BCP-47 tag the recognizer identified in AUTO mode, when supported. */
        val detectedLanguage: String? = null,
    )

    private val _state = MutableStateFlow(State(available = SpeechRecognizer.isRecognitionAvailable(context)))
    val state: StateFlow<State> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    /** True when the currently-created recognizer is the API 34+ on-device engine. */
    private var usingOnDeviceEngine = false

    /**
     * Set once the on-device engine reports it has no usable language model
     * (ERROR_LANGUAGE_*). From then on AUTO stops trying it and uses the default
     * engine, so we don't keep hitting the same dead end.
     */
    private var onDeviceEngineUnusable = false

    /** The language the in-flight session was started with (for error-restarts). */
    private var currentLanguage: VoiceLanguage = VoiceLanguage.AUTO

    fun startListening(language: VoiceLanguage) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update {
                it.copy(available = false, isListening = false, error = "Speech recognition isn't available on this device.")
            }
            return
        }
        currentLanguage = language

        // AUTO uses the on-device detector when we can; a specific language uses the
        // default (cloud) recognizer, which reliably transcribes the chosen language.
        val useOnDevice = language == VoiceLanguage.AUTO &&
            !onDeviceEngineUnusable &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

        // (Re)create the recognizer only when the required engine changes.
        if (recognizer == null || usingOnDeviceEngine != useOnDevice) {
            recognizer?.destroy()
            recognizer = createRecognizer(useOnDevice).also { it.setRecognitionListener(listener) }
            usingOnDeviceEngine = useOnDevice
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            when {
                useOnDevice -> {
                    // Let the engine identify the spoken language (unconstrained) and
                    // switch to it — do NOT pin EXTRA_LANGUAGE.
                    putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                    putExtra(
                        RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH,
                        RecognizerIntent.LANGUAGE_SWITCH_BALANCED,
                    )
                }
                language.tag != null -> {
                    // Reliable path: transcribe the explicitly chosen language.
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.tag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.tag)
                }
                else -> {
                    // AUTO but no on-device detection: fall back to the user's Google
                    // voice-input default rather than force the device locale.
                }
            }
        }
        _state.update {
            it.copy(
                available = true, isListening = true, error = null,
                finalText = null, partialText = "", detectedLanguage = null,
            )
        }
        recognizer?.startListening(intent)
    }

    private fun createRecognizer(onDevice: Boolean): SpeechRecognizer =
        if (onDevice) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
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

        // Fired by the on-device engine (API 34+) when it identifies the language.
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
                // On-device auto-detect has no model installed: give up on it and
                // retry the same request on the default engine (which, for AUTO,
                // uses the user's voice-input default instead of erroring).
                onDeviceEngineUnusable = true
                recognizer?.destroy()
                recognizer = null
                usingOnDeviceEngine = false
                if (_state.value.isListening) {
                    startListening(currentLanguage)
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
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again, or pick a language"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Auto didn't work — pick a language"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Auto didn't work — pick a language"
        else -> "Recognition error ($code)"
    }
}
