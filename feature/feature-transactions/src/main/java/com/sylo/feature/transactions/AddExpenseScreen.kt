package com.sylo.feature.transactions

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.ui.component.SectionLabel
import com.sylo.core.ui.component.SyloAmountField
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.component.SyloPrimaryButton
import com.sylo.core.ui.component.currencySymbol
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing
import java.io.File
import java.util.UUID

private data class Category(val label: String, val icon: ImageVector)

private val categories = listOf(
    Category("Food", Icons.Filled.LocalCafe),
    Category("Travel", Icons.Filled.CardTravel),
    Category("Shopping", Icons.Filled.ShoppingBag),
    Category("Leisure", Icons.Filled.Nightlife),
)

/** Add-expense form presented as a modal bottom sheet over the current screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddExpenseViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first().label) }
    var note by remember { mutableStateOf("") }
    var receiptPath by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Photo Picker — no runtime permission needed; returns a content Uri we copy locally.
    val pickReceipt = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) receiptPath = copyReceiptToInternal(context, uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SyloSpacing.containerMargin)
            .padding(bottom = SyloSpacing.stackMd)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Text(
            "New Expense",
            style = MaterialTheme.typography.titleLarge,
            color = SyloBrandCyan,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(SyloSpacing.stackMd))
        SyloCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SyloSpacing.stackMd, vertical = SyloSpacing.stackMd),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SectionLabel("Amount")
                Spacer(Modifier.height(SyloSpacing.stackSm))
                SyloAmountField(
                    amount = amount,
                    onAmountChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    currencySymbol = currencySymbol(currency),
                )
            }
        }

        Spacer(Modifier.height(SyloSpacing.stackLg))
        SectionLabel("Category")
        Spacer(Modifier.height(SyloSpacing.stackSm))
        Row(horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
            categories.forEach { cat ->
                CategoryChip(
                    category = cat,
                    selected = cat.label == selectedCategory,
                    onClick = { selectedCategory = cat.label },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(SyloSpacing.stackMd))
        val today = remember {
            java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                .format(System.currentTimeMillis())
        }
        FieldRow(label = "Date", value = today, icon = Icons.Filled.CalendarMonth)
        Spacer(Modifier.height(SyloSpacing.stackSm))
        FieldRow(label = "Payment Method", value = "Personal Debit", icon = Icons.Filled.CreditCard)

        Spacer(Modifier.height(SyloSpacing.stackMd))
        SectionLabel("Note")
        Spacer(Modifier.height(SyloSpacing.stackSm))
        TextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth().height(96.dp),
            placeholder = { Text("What was this for?") },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
            ),
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(Modifier.height(SyloSpacing.stackMd))
        ReceiptAttach(
            receiptPath = receiptPath,
            onPick = {
                pickReceipt.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemove = { receiptPath = null },
        )

        Spacer(Modifier.height(SyloSpacing.stackLg))
        SyloPrimaryButton(
            text = "Save Expense",
            onClick = { viewModel.save(amount, selectedCategory, note, receiptPath, onSaved) },
            enabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0,
        )
        Spacer(Modifier.height(SyloSpacing.stackMd))
    }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) SyloBrandCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer
    val tint = if (selected) SyloBrandCyan else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .then(if (selected) Modifier.border(1.dp, SyloBrandCyan, MaterialTheme.shapes.medium) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = SyloSpacing.stackSm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(category.icon, contentDescription = null, tint = tint)
        Text(category.label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun FieldRow(label: String, value: String, icon: ImageVector) {
    SyloCard {
        Column(Modifier.padding(SyloSpacing.stackMd)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(SyloSpacing.stackSm))
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun ReceiptAttach(
    receiptPath: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    if (receiptPath == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                .clickable(onClick = onPick)
                .padding(SyloSpacing.stackLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm),
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Attach Receipt (Optional)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Downsampled preview of the picked receipt image.
    val bitmap = remember(receiptPath) {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        runCatching { BitmapFactory.decodeFile(receiptPath, opts) }.getOrNull()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(MaterialTheme.shapes.large)
            .border(1.dp, SyloBrandCyan.copy(alpha = 0.5f), MaterialTheme.shapes.large),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Attached receipt",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clickable(onClick = onPick),
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(SyloSpacing.stackSm)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Remove receipt", tint = Color.White)
        }
    }
}

/** Copies a picked image [uri] into app-internal storage and returns its absolute path. */
private fun copyReceiptToInternal(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    file.absolutePath
}.getOrNull()
