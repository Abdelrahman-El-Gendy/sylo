package com.sylo.feature.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.component.SyloPrimaryButton
import com.sylo.core.ui.theme.MonoNumberStyle
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing

@Composable
fun VoiceReviewRoute(
    transcript: String,
    onConfirmSave: () -> Unit,
    onEditDetails: () -> Unit,
    viewModel: VoiceReviewViewModel = hiltViewModel(),
) {
    LaunchedEffect(transcript) { viewModel.setTranscript(transcript) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(SyloSpacing.containerMargin),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                Spacer(Modifier.height(SyloSpacing.stackMd))
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(SyloBrandCyan),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Spacer(Modifier.height(SyloSpacing.stackMd))
                Text("Review Transaction", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(SyloSpacing.stackSm))
                Text(
                    "I heard: \"${uiState.parsed.note}\". Is this correct?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(SyloSpacing.stackMd))
                SyloCard {
                    Column(Modifier.padding(SyloSpacing.stackMd)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Amount", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(uiState.parsed.amountDisplay, style = MaterialTheme.typography.headlineMedium, color = SyloBrandCyan, fontWeight = FontWeight.Bold)
                                    Text(" ${uiState.currency.ifBlank { uiState.parsed.currency }}", style = MonoNumberStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                                }
                            }
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                Icon(categoryIcon(uiState.parsed.category), contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.padding(8.dp))
                            }
                        }
                        Spacer(Modifier.height(SyloSpacing.stackMd))
                        Row(Modifier.fillMaxWidth()) {
                            LabeledValue("Category", uiState.parsed.category, Modifier.weight(1f))
                            LabeledValue("Saved to", "Encrypted local DB", Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(SyloSpacing.stackMd))
                if (!uiState.canSave) {
                    Text(
                        "No amount detected. Tap \"Edit Details\" to enter it manually.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(SyloSpacing.stackSm))
                }
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = SyloBrandCyan)
                } else {
                    SyloPrimaryButton(
                        text = "Confirm & Save",
                        onClick = { viewModel.confirmAndSave(onConfirmSave) },
                        enabled = uiState.canSave,
                    )
                }
                Spacer(Modifier.height(SyloSpacing.stackSm))
                OutlinedButton(onClick = onEditDetails, modifier = Modifier.fillMaxWidth()) {
                    Text("Edit Details")
                }
                Spacer(Modifier.height(SyloSpacing.stackSm))
            }
        }
    }
}

/** Maps a parsed category to a representative icon for the review card. */
private fun categoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector = when (category) {
    "Food" -> Icons.Filled.Restaurant
    "Transport" -> Icons.Filled.DirectionsCar
    "Shopping" -> Icons.Filled.ShoppingBag
    "Entertainment" -> Icons.Filled.Movie
    "Bills" -> Icons.AutoMirrored.Filled.ReceiptLong
    else -> Icons.Filled.Payments
}

@Composable
private fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
