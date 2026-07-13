package com.sylo.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.core.ui.component.SyloTopBar
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing
import com.sylo.feature.voice.speech.VoiceLanguage
import kotlin.math.abs

@Composable
fun VoiceCaptureRoute(
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    viewModel: VoiceCaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    val transcript = state.finalText?.takeIf { it.isNotBlank() } ?: state.partialText
    val category = if (transcript.isBlank()) null else viewModel.currentCategory()
    val insight = remember(transcript, transactions, currency) {
        spendingInsight(category, transactions, currency)
    }

    VoiceCaptureScreen(
        transcript = transcript,
        isListening = state.isListening,
        available = state.available && hasPermission,
        error = if (!hasPermission) "Microphone permission is required" else state.error,
        category = category,
        insight = insight,
        language = language,
        onLanguageChange = viewModel::setLanguage,
        onHoldStart = {
            if (hasPermission) viewModel.startListening()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onHoldEnd = { viewModel.stopListening() },
        onConfirm = { if (transcript.isNotBlank()) onConfirm(transcript) },
        onCancel = onCancel,
    )
}

@Composable
private fun VoiceCaptureScreen(
    transcript: String,
    isListening: Boolean,
    available: Boolean,
    error: String?,
    category: String?,
    insight: String?,
    language: VoiceLanguage,
    onLanguageChange: (VoiceLanguage) -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val canConfirm = transcript.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SyloSpacing.containerMargin, vertical = SyloSpacing.stackMd),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SyloTopBar()

        Spacer(Modifier.height(SyloSpacing.stackMd))
        LanguageToggle(
            selected = language,
            enabled = !isListening,
            onSelect = onLanguageChange,
        )

        Spacer(Modifier.weight(0.4f))
        MicRing(
            isListening = isListening,
            enabled = available,
            error = error,
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

        Spacer(Modifier.height(SyloSpacing.stackMd))
        Waveform(active = isListening)

        Spacer(Modifier.height(SyloSpacing.stackLg))
        ContextCard(category = category, insight = insight)

        Spacer(Modifier.weight(0.6f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CircleAction(Icons.Filled.Close, "Cancel", MaterialTheme.colorScheme.surfaceContainerHighest, enabled = true, onClick = onCancel)
            CircleAction(Icons.Filled.Check, "Confirm", SyloBrandCyan, enabled = canConfirm, onClick = onConfirm)
        }
        Spacer(Modifier.height(SyloSpacing.stackMd))
    }
}

/** The concentric ring + press-and-hold mic. Holding records; releasing ends capture. */
@Composable
private fun MicRing(
    isListening: Boolean,
    enabled: Boolean,
    error: String?,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    val ringColor = if (isListening) SyloBrandCyan else MaterialTheme.colorScheme.outlineVariant
    val scale by animateFloatAsState(if (isListening) 1.08f else 1f, label = "micScale")

    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        // Outer ring
        Box(
            Modifier
                .size(240.dp)
                .clip(CircleShape)
                .border(2.dp, ringColor, CircleShape),
        )
        // Inner glowing mic — the hold target
        Box(
            modifier = Modifier
                .scale(scale)
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(SyloBrandCyan, SyloBrandCyan.copy(alpha = 0.55f))),
                )
                .pointerInput(enabled) {
                    detectTapGestures(
                        onPress = {
                            onHoldStart()
                            tryAwaitRelease()
                            onHoldEnd()
                        },
                    )
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
        // Status pill straddling the top of the ring
        StatusPill(
            isListening = isListening,
            error = error,
            available = enabled,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

/** EN / AR segmented toggle for the dictation language, disabled mid-recording. */
@Composable
private fun LanguageToggle(
    selected: VoiceLanguage,
    enabled: Boolean,
    onSelect: (VoiceLanguage) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        VoiceLanguage.entries.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) SyloBrandCyan else Color.Transparent)
                    .then(if (enabled) Modifier.clickable { onSelect(option) } else Modifier)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

@Composable
private fun Waveform(active: Boolean) {
    val heights = listOf(12, 22, 34, 18, 40, 26, 30, 16, 24, 12)
    val color = if (active) SyloBrandCyan else MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEach { h ->
            Box(Modifier.width(4.dp).height(h.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun CircleAction(icon: ImageVector, label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (enabled) color else color.copy(alpha = 0.4f))
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
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
