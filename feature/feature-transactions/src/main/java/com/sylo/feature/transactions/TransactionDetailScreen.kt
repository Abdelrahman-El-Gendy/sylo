package com.sylo.feature.transactions

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.core.ui.component.SectionLabel
import com.sylo.core.ui.component.StatusDot
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.component.formatAmountLabel
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloIncomeGreen
import com.sylo.core.ui.theme.SyloSpacing
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TransactionDetailRoute(
    transactionId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(transactionId) { viewModel.load(transactionId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SyloSpacing.containerMargin, vertical = SyloSpacing.stackMd),
        verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackMd),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Sylo", style = MaterialTheme.typography.titleLarge, color = SyloBrandCyan)
            Box(Modifier.size(48.dp))
        }

        val tx = uiState.transaction
        when {
            uiState.loading -> Box(Modifier.fillMaxWidth().padding(top = 96.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SyloBrandCyan)
            }
            tx == null -> Box(Modifier.fillMaxWidth().padding(top = 96.dp), contentAlignment = Alignment.Center) {
                Text("Transaction not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> DetailContent(tx, onEdit) { viewModel.delete(tx.id, onBack) }
        }
    }
}

@Composable
private fun DetailContent(tx: TransactionEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    val amountColor = if (tx.amountMinor > 0) SyloIncomeGreen else SyloBrandCyan
    val dateLabel = SimpleDateFormat("EEEE, MMM d · hh:mm a", Locale.getDefault()).format(tx.timestampEpochMillis)

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
        Text(tx.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        CategoryChip(tx.category.uppercase())
        Text(formatAmountLabel(tx.amountMinor, tx.currency), style = MaterialTheme.typography.displayLarge, color = amountColor, fontWeight = FontWeight.Bold)
        Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Spacer(Modifier.size(SyloSpacing.stackSm))
    SyloCard {
        Column(Modifier.padding(SyloSpacing.stackMd), verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
            DetailRow("Category") { Text(tx.category, style = MaterialTheme.typography.bodyMedium) }
            tx.status?.let { status ->
                DetailRow("Status") { StatusDot(status) }
            }
            tx.note?.takeIf { it.isNotBlank() }?.let { note ->
                Column {
                    SectionLabel("Note")
                    Text(note, style = MaterialTheme.typography.bodyMedium)
                }
            }
            tx.receiptPath?.takeIf { File(it).exists() }?.let { path ->
                Column {
                    SectionLabel("Receipt")
                    Spacer(Modifier.size(SyloSpacing.stackSm))
                    ReceiptImage(path)
                }
            }
        }
    }

    Spacer(Modifier.size(SyloSpacing.stackSm))
    Row(horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
        OutlinedButton(onClick = onDelete, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Text("Delete", modifier = Modifier.padding(start = 4.dp))
        }
        Button(
            onClick = onEdit,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Text("Edit", modifier = Modifier.padding(start = 4.dp))
        }
    }
    Spacer(Modifier.size(SyloSpacing.stackMd))
}

@Composable
private fun ReceiptImage(path: String) {
    val bitmap = remember(path) {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        runCatching { BitmapFactory.decodeFile(path, opts) }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Receipt",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(MaterialTheme.shapes.medium),
        )
    }
}

@Composable
private fun CategoryChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun DetailRow(label: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        trailing()
    }
}
