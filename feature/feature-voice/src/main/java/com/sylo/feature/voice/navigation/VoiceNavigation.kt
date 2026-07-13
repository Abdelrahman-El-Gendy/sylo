package com.sylo.feature.voice.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sylo.core.navigation.VoiceCaptureRoute
import com.sylo.core.navigation.VoiceReviewRoute
import com.sylo.feature.voice.VoiceCaptureRoute as VoiceCaptureScreenRoute
import com.sylo.feature.voice.VoiceReviewRoute as VoiceReviewScreenRoute

/** The voice capture flow: capture (hold-to-talk) -> review (carries the recognized text). */
fun EntryProviderScope<NavKey>.voiceEntries(
    onCaptured: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirmSave: () -> Unit,
    onEditDetails: () -> Unit,
) {
    entry<VoiceCaptureRoute> {
        VoiceCaptureScreenRoute(onConfirm = onCaptured, onCancel = onCancel)
    }
    entry<VoiceReviewRoute> { key ->
        VoiceReviewScreenRoute(
            transcript = key.transcript,
            onConfirmSave = onConfirmSave,
            onEditDetails = onEditDetails,
        )
    }
}
