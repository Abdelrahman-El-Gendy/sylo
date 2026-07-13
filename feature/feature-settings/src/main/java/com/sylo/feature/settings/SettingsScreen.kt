package com.sylo.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.util.UUID
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.ui.component.SectionLabel
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloIncomeGreen
import com.sylo.core.ui.theme.SyloSpacing

@Composable
fun SettingsRoute(
    onChangePin: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val biometricAvailable = viewModel.biometricAvailable
    val context = LocalContext.current

    var autoCaptureEnabled by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        autoCaptureEnabled = isNotificationAccessGranted(context)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Profile photo picker — copies the chosen image into internal storage.
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.saveProfilePhoto(copyProfilePhotoToInternal(context, uri))
    }

    // Bank-SMS import needs READ_SMS (+ POST_NOTIFICATIONS on 33+). Only flip the
    // preference on once READ_SMS is actually granted.
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.READ_SMS] == true) {
            viewModel.setSmsImportEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("SMS permission is needed to import bank statements") }
        }
    }

    var showNameDialog by remember { mutableStateOf(false) }
    if (showNameDialog) {
        EditNameDialog(
            initial = uiState.displayName,
            onDismiss = { showNameDialog = false },
            onSave = { name ->
                viewModel.saveDisplayName(name)
                showNameDialog = false
                scope.launch { snackbarHostState.showSnackbar("Profile updated") }
            },
        )
    }

    var showBalanceSheet by remember { mutableStateOf(false) }
    if (showBalanceSheet) {
        BalanceCurrencySheet(
            onDismiss = { showBalanceSheet = false },
            onSaved = {
                showBalanceSheet = false
                scope.launch { snackbarHostState.showSnackbar("Balance updated") }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SyloSpacing.containerMargin),
        verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackMd),
    ) {
        Spacer(Modifier.height(SyloSpacing.stackSm))
        Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        ProfileHeader(
            name = uiState.displayName,
            photoPath = uiState.profilePhotoPath,
            onEditPhoto = {
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onEditName = { showNameDialog = true },
        )
        SyloProCard()
        StatsGrid(uiState.stats)

        SectionLabel("Identity")
        SyloCard {
            SettingsRow(
                icon = Icons.Filled.VerifiedUser,
                title = "Verify email",
                subtitle = uiState.verifiedEmail?.let { "Verified · $it" }
                    ?: "Confirm your email with Google — no OTP",
                trailing = when {
                    uiState.verifyingEmail -> ({
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = SyloBrandCyan,
                        )
                    })
                    uiState.verifiedEmail != null -> ({ VerifiedPill() })
                    else -> null
                },
                onClick = if (uiState.verifyingEmail) {
                    null
                } else {
                    {
                        viewModel.verifyEmail(context) { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    }
                },
            )
        }

        SectionLabel("Security")
        SyloCard {
            Column {
                SettingsRow(Icons.Filled.Lock, "Change PIN", onClick = onChangePin)
                SettingsRow(
                    icon = Icons.Filled.Fingerprint,
                    title = "Biometrics",
                    subtitle = when {
                        !biometricAvailable -> "Not available on this device"
                        uiState.dbBiometricBound -> "Unlocks your encrypted database"
                        else -> "Use fingerprint or face to unlock"
                    },
                    trailing = {
                        Switch(
                            checked = uiState.biometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) },
                            enabled = biometricAvailable,
                        )
                    },
                )
                SettingsRow(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Auto-capture payments",
                    subtitle = if (autoCaptureEnabled) "Reading bank & wallet notifications" else "Tap to grant notification access",
                    trailing = if (autoCaptureEnabled) ({ ActivePill() }) else null,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
                SettingsRow(
                    icon = Icons.Filled.Sms,
                    title = "Import bank SMS",
                    subtitle = if (uiState.smsImportEnabled) {
                        "Scanning today's bank messages"
                    } else {
                        "Read bank SMS to auto-log expenses"
                    },
                    trailing = {
                        Switch(
                            checked = uiState.smsImportEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (context.checkSelfPermission(Manifest.permission.READ_SMS)
                                        == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        viewModel.setSmsImportEnabled(true)
                                    } else {
                                        smsPermissionLauncher.launch(smsImportPermissions())
                                    }
                                } else {
                                    viewModel.setSmsImportEnabled(false)
                                }
                            },
                        )
                    },
                )
            }
        }

        SectionLabel("Preferences")
        SyloCard {
            SettingsRow(
                icon = Icons.Filled.AttachMoney,
                title = "Balance & Currency",
                subtitle = "Set your opening balance and currency",
                onClick = { showBalanceSheet = true },
            )
        }

        SectionLabel("Support")
        SyloCard {
            Column {
                SettingsRow(Icons.Filled.HelpOutline, "Help Center", onClick = { context.openUrl(HELP_URL) })
                SettingsRow(Icons.Filled.Description, "Terms of Service", onClick = { context.openUrl(TERMS_URL) })
            }
        }

        Spacer(Modifier.height(SyloSpacing.stackSm))
        SyloCard {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Logout",
                tint = MaterialTheme.colorScheme.error,
                showChevron = false,
                onClick = onLoggedOut,
            )
        }

        Text(
            "SYLO FINANCE ENGINE V2.4.1\nIntelligent. Secure. Trustworthy.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SyloSpacing.sectionGap))
    }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    photoPath: String?,
    onEditPhoto: () -> Unit,
    onEditName: () -> Unit,
) {
    val photoBitmap = remember(photoPath) {
        photoPath?.takeIf { File(it).exists() }?.let {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            runCatching { BitmapFactory.decodeFile(it, opts) }.getOrNull()
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(2.dp, SyloBrandCyan, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onEditPhoto),
                contentAlignment = Alignment.Center,
            ) {
                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = "Profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.size(44.dp))
                }
            }
            // Camera badge indicates the avatar is tappable to change the photo.
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SyloBrandCyan)
                    .clickable(onClick = onEditPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Change photo",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clickable(onClick = onEditName),
        ) {
            Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.Edit, contentDescription = "Edit name", tint = SyloBrandCyan, modifier = Modifier.size(18.dp))
        }
        Text(
            "Secured locally with PIN + biometrics",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditNameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your name") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Enter your name") },
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Copies a picked image [uri] into app-internal storage and returns its absolute path. */
private fun copyProfilePhotoToInternal(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.filesDir, "profile").apply { mkdirs() }
    val file = File(dir, "avatar-${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    file.absolutePath
}.getOrNull()

@Composable
private fun SyloProCard() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().border(1.dp, SyloBrandCyan.copy(alpha = 0.4f), MaterialTheme.shapes.large),
    ) {
        Row(
            Modifier.padding(SyloSpacing.stackMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm),
        ) {
            Icon(Icons.Filled.Diamond, contentDescription = null, tint = SyloBrandCyan)
            Column(Modifier.weight(1f)) {
                Text("Sylo Pro", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Advanced insights & unlimited voice capture",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = RoundedCornerShape(50), color = SyloBrandCyan) {
                Text(
                    "Upgrade",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(stats: ProfileStats) {
    Column(verticalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
            StatCard("Total Saved", stats.totalBalance, Icons.Filled.AccountBalanceWallet, SyloIncomeGreen, Modifier.weight(1f))
            StatCard("Days Tracking", stats.daysTracking, Icons.Filled.CalendarMonth, SyloBrandCyan, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
            StatCard("Transactions", stats.transactions, Icons.AutoMirrored.Filled.ReceiptLong, SyloBrandCyan, Modifier.weight(1f))
            StatCard("This Month", stats.thisMonth, Icons.Filled.Savings, SyloBrandCyan, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    SyloCard(modifier = modifier) {
        Column(Modifier.padding(SyloSpacing.stackMd)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(SyloSpacing.stackSm))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: androidx.compose.ui.graphics.Color = SyloBrandCyan,
    showChevron: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(SyloSpacing.stackMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackMd),
    ) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(8.dp).size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tint == MaterialTheme.colorScheme.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            trailing != null -> trailing()
            showChevron -> Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivePill() {
    Surface(shape = RoundedCornerShape(50), color = SyloBrandCyan.copy(alpha = 0.15f)) {
        Text(
            "ACTIVE",
            style = MaterialTheme.typography.labelSmall,
            color = SyloBrandCyan,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun VerifiedPill() {
    Surface(shape = RoundedCornerShape(50), color = SyloBrandCyan.copy(alpha = 0.15f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = SyloBrandCyan, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(4.dp))
            Text("VERIFIED", style = MaterialTheme.typography.labelSmall, color = SyloBrandCyan)
        }
    }
}

private fun isNotificationAccessGranted(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    return flat.split(":").any { it.contains(context.packageName) }
}

/** READ_SMS to read statements, plus POST_NOTIFICATIONS (API 33+) to surface captures. */
private fun smsImportPermissions(): Array<String> = buildList {
    add(Manifest.permission.READ_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

// TODO: point these at the real Sylo help/legal pages before shipping.
private const val HELP_URL = "https://sylo.app/help"
private const val TERMS_URL = "https://sylo.app/terms"

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show()
    }
}
