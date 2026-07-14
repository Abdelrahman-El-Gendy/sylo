package com.sylo.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.core.ui.component.SyloPrimaryButton
import com.sylo.core.ui.component.SyloTopBar
import com.sylo.core.ui.component.currencySymbol
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing
import kotlin.math.abs

@Composable
fun VoiceCaptureRoute(
    onSaved: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: VoiceCaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    // Live text (partial while speaking); the final result gates the review sheet.
    val liveText = state.finalText?.takeIf { it.isNotBlank() } ?: state.partialText
    val recorded = state.finalText?.takeIf { it.isNotBlank() }
    val category = if (liveText.isBlank()) null else viewModel.currentCategory()
    val insight = remember(liveText, transactions, currency) {
        spendingInsight(category, transactions, currency)
    }

    // Android's SpeechRecognizer reports loudness in dB (~0 quiet … ~10 loud);
    // normalize to 0..1 so the waveform fill can react to the voice.
    val level = if (state.isListening) (state.rms / 10f).coerceIn(0f, 1f) else 0f

    VoiceCaptureScreen(
        transcript = liveText,
        isListening = state.isListening,
        available = state.available && hasPermission,
        error = if (!hasPermission) "Microphone permission is required" else state.error,
        category = category,
        insight = insight,
        level = level,
        onHoldStart = {
            if (hasPermission) viewModel.startListening()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onHoldEnd = { viewModel.stopListening() },
    )

    // Once a recording is captured, review it in a bottom sheet: submit or edit.
    if (recorded != null && !state.isListening) {
        ReviewSheet(
            transcript = recorded,
            amount = viewModel.currentAmount(),
            currency = currency,
            category = viewModel.currentCategory(),
            canSubmit = viewModel.canSubmit(),
            saving = saving,
            onSubmit = { viewModel.submit(onSaved) },
            onEdit = { onEdit(recorded) },
            onDismiss = { viewModel.discard() },
        )
    }
}

@Composable
private fun VoiceCaptureScreen(
    transcript: String,
    isListening: Boolean,
    available: Boolean,
    error: String?,
    category: String?,
    insight: String?,
    level: Float,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SyloSpacing.containerMargin, vertical = SyloSpacing.stackMd),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SyloTopBar()

        Spacer(Modifier.weight(0.2f))
        StatusPill(isListening = isListening, error = error, available = available)

        Spacer(Modifier.height(SyloSpacing.stackLg))
        // Static waveform silhouette; the cyan fill rises/falls with the voice level.
        Waveform(
            active = isListening,
            level = level,
            modifier = Modifier.fillMaxWidth().height(120.dp),
        )

        Spacer(Modifier.height(SyloSpacing.stackLg))
        // The mic ring is the press-and-hold target (no animation).
        MicRing(
            isListening = isListening,
            enabled = available,
            onHoldStart = onHoldStart,
            onHoldEnd = onHoldEnd,
        )

        Spacer(Modifier.height(SyloSpacing.stackLg))
        Text(
            text = when {
                transcript.isNotBlank() -> "\"$transcript\""
                isListening -> "Listening…"
                else -> "Hold to speak"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(SyloSpacing.stackSm))
        Text(
            text = when {
                category != null -> "Watching for category: $category"
                else -> "Press and hold, then release to add"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(0.4f))
        ContextCard(category = category, insight = insight)
        Spacer(Modifier.height(SyloSpacing.stackMd))
    }
}

/** The concentric ring + press-and-hold mic. Holding records; releasing ends capture. */
@Composable
private fun MicRing(
    isListening: Boolean,
    enabled: Boolean,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    val ringColor = if (isListening) SyloBrandCyan else MaterialTheme.colorScheme.outlineVariant
    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        // Outer ring
        Box(
            Modifier
                .size(180.dp)
                .clip(CircleShape)
                .border(2.dp, ringColor, CircleShape),
        )
        // Inner glowing mic — the hold target
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(SyloBrandCyan, SyloBrandCyan.copy(alpha = 0.55f))),
                )
                .pointerInput(enabled) {
                    if (enabled) {
                        detectTapGestures(
                            onPress = {
                                onHoldStart()
                                tryAwaitRelease()
                                onHoldEnd()
                            },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = "Hold to record",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun StatusPill(isListening: Boolean, error: String?, available: Boolean, modifier: Modifier = Modifier) {
    val (label, dot) = when {
        error != null -> error.uppercase() to MaterialTheme.colorScheme.error
        !available -> "UNAVAILABLE" to MaterialTheme.colorScheme.error
        isListening -> "LISTENING..." to SyloBrandCyan
        else -> "HOLD TO SPEAK" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/**
 * Per-bar heights of the static waveform silhouette (fractions of the full height).
 * Symmetric, peaking in the middle — the shape itself never changes.
 */
private val WAVEFORM_WEIGHTS = listOf(
    0.16f, 0.28f, 0.20f, 0.42f, 0.32f, 0.58f, 0.46f, 0.74f, 0.54f, 0.88f,
    0.64f, 1f, 0.70f, 1f, 0.64f, 0.88f, 0.54f, 0.74f, 0.46f, 0.58f,
    0.32f, 0.42f, 0.20f, 0.28f, 0.16f,
)

/**
 * Full-width, static waveform silhouette. The bar heights are fixed (the shape never
 * changes); a brand-cyan fill rises from the bottom of every bar with the live
 * recording [level] and recedes ("unfills") as it goes quiet. Visual only — the mic
 * ring handles the hold gesture.
 */
@Composable
private fun Waveform(active: Boolean, level: Float, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(
        targetValue = if (active) level.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "waveFill",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WAVEFORM_WEIGHTS.forEach { weight ->
            // Dim static track sets the fixed bar height...
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(weight)
                    .clip(CircleShape)
                    .background(trackColor),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // ...and the cyan fill rises from the bottom with the recording level.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fill)
                        .clip(CircleShape)
                        .background(SyloBrandCyan),
                )
            }
        }
    }
}

/** Bottom sheet shown after a recording: submit it as-is, or open the editor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSheet(
    transcript: String,
    amount: String,
    currency: String,
    category: String,
    canSubmit: Boolean,
    saving: Boolean,
    onSubmit: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SyloSpacing.containerMargin)
                .padding(bottom = SyloSpacing.stackLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(SyloBrandCyan),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Spacer(Modifier.height(SyloSpacing.stackMd))
            Text("Review expense", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(SyloSpacing.stackSm))
            Text(
                "I heard: \"$transcript\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(SyloSpacing.stackMd))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(SyloSpacing.stackMd),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${currencySymbol(currency)}$amount",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SyloBrandCyan,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Icon(categoryIcon(category), contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.padding(8.dp))
                        }
                        Text(category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (!canSubmit) {
                Spacer(Modifier.height(SyloSpacing.stackSm))
                Text(
                    "No amount detected — tap Edit to enter it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(SyloSpacing.stackMd))
            SyloPrimaryButton(
                text = if (saving) "Saving…" else "Submit",
                onClick = onSubmit,
                enabled = canSubmit && !saving,
            )
            Spacer(Modifier.height(SyloSpacing.stackSm))
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit")
            }
        }
    }
}

/** The "PREVIOUS CONTEXT" card — a spending insight for the category, or a usage hint. */
@Composable
private fun ContextCard(category: String?, insight: String?) {
    val label = if (insight != null) "PREVIOUS CONTEXT" else "HOW IT WORKS"
    val message = insight
        ?: "Hold the mic and say the amount and what it was for — e.g. \"Coffee 45\"."
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(SyloSpacing.stackMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Icon(
                    categoryIcon(category),
                    contentDescription = null,
                    tint = SyloBrandCyan,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Spacer(Modifier.width(SyloSpacing.stackMd))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Average past spend for [category], phrased as a context hint, or null if no history. */
private fun spendingInsight(
    category: String?,
    transactions: List<TransactionEntity>,
    currency: String,
): String? {
    if (category == null || category == "General") return null
    val same = transactions.filter { it.category.equals(category, ignoreCase = true) }
    if (same.isEmpty()) return null
    val avg = same.map { abs(it.amountMinor) }.average() / 100.0
    return "You usually spend ~%.0f %s on %s.".format(avg, currency, category.lowercase())
}

private fun categoryIcon(category: String?): ImageVector = when (category) {
    "Food" -> Icons.Filled.Restaurant
    "Transport" -> Icons.Filled.DirectionsCar
    "Shopping" -> Icons.Filled.ShoppingBag
    "Entertainment" -> Icons.Filled.Movie
    "Bills" -> Icons.AutoMirrored.Filled.ReceiptLong
    else -> Icons.Filled.Payments
}
