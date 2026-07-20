package com.sylo.feature.settings

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File
import java.util.UUID
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.ui.R
import com.sylo.core.ui.component.SectionLabel
import com.sylo.core.ui.component.SyloCard
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloIncomeGreen
import com.sylo.core.ui.theme.SyloSpacing
import com.sylo.core.ui.theme.ThemeMode

@Composable
fun SettingsRoute(
    onChangePin: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val languageTag by viewModel.languageTag.collectAsStateWithLifecycle()
    val biometricAvailable = viewModel.biometricAvailable
    val context = LocalContext.current

    var autoCaptureEnabled by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        autoCaptureEnabled = isNotificationAccessGranted(context)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val smsPermissionNeededMessage = stringResource(R.string.settings_sms_permission_needed)
    val profileUpdatedMessage = stringResource(R.string.settings_profile_updated)
    val balanceUpdatedMessage = stringResource(R.string.settings_balance_updated)

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
            scope.launch { snackbarHostState.showSnackbar(smsPermissionNeededMessage) }
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
                scope.launch { snackbarHostState.showSnackbar(profileUpdatedMessage) }
            },
        )
    }

    var showBalanceSheet by remember { mutableStateOf(false) }
    if (showBalanceSheet) {
        BalanceCurrencySheet(
            onDismiss = { showBalanceSheet = false },
            onSaved = {
                showBalanceSheet = false
                scope.launch { snackbarHostState.showSnackbar(balanceUpdatedMessage) }
            },
        )
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    if (showThemeDialog) {
        ThemeDialog(
            selected = themeMode,
            onSelect = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false },
        )
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    if (showLanguageDialog) {
        LanguageDialog(
            selected = languageTag,
            onSelect = { tag ->
                viewModel.setLanguageTag(tag)
                showLanguageDialog = false
                // The base context locale is fixed at attachBaseContext time, so the
                // Activity must be recreated for the new language to take effect.
                context.findActivity()?.recreate()
            },
            onDismiss = { showLanguageDialog = false },
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
        Text(stringResource(R.string.settings_profile), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

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

        SectionLabel(stringResource(R.string.settings_section_identity))
        SyloCard {
            SettingsRow(
                icon = Icons.Filled.VerifiedUser,
                title = stringResource(R.string.settings_verify_email),
                subtitle = uiState.verifiedEmail?.let { stringResource(R.string.settings_verified_prefix, it) }
                    ?: stringResource(R.string.settings_verify_email_subtitle),
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

        SectionLabel(stringResource(R.string.settings_section_security))
        SyloCard {
            Column {
                SettingsRow(Icons.Filled.Lock, stringResource(R.string.settings_change_pin), onClick = onChangePin)
                SettingsRow(
                    icon = Icons.Filled.Fingerprint,
                    title = stringResource(R.string.settings_biometrics),
                    subtitle = when {
                        !biometricAvailable -> stringResource(R.string.settings_biometrics_unavailable)
                        uiState.dbBiometricBound -> stringResource(R.string.settings_biometrics_bound)
                        else -> stringResource(R.string.settings_biometrics_use)
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
                    title = stringResource(R.string.settings_autocapture),
                    subtitle = stringResource(
                        if (autoCaptureEnabled) R.string.settings_autocapture_on else R.string.settings_autocapture_off,
                    ),
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
                    title = stringResource(R.string.settings_import_sms),
                    subtitle = stringResource(
                        if (uiState.smsImportEnabled) R.string.settings_import_sms_on else R.string.settings_import_sms_off,
                    ),
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

        SectionLabel(stringResource(R.string.settings_section_preferences))
        SyloCard {
            Column {
                SettingsRow(
                    icon = Icons.Filled.AttachMoney,
                    title = stringResource(R.string.settings_balance_currency),
                    subtitle = stringResource(R.string.settings_balance_currency_subtitle),
                    onClick = { showBalanceSheet = true },
                )
                SettingsRow(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = themeMode.label(),
                    onClick = { showThemeDialog = true },
                )
                SettingsRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = languageLabel(languageTag),
                    onClick = { showLanguageDialog = true },
                )
            }
        }

        SectionLabel(stringResource(R.string.settings_section_support))
        SyloCard {
            Column {
                SettingsRow(Icons.Filled.HelpOutline, stringResource(R.string.settings_help_center), onClick = { context.openUrl(HELP_URL) })
                SettingsRow(Icons.Filled.Description, stringResource(R.string.settings_terms), onClick = { context.openUrl(TERMS_URL) })
            }
        }

        Spacer(Modifier.height(SyloSpacing.stackSm))
        SyloCard {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(R.string.settings_logout),
                tint = MaterialTheme.colorScheme.error,
                showChevron = false,
                onClick = onLoggedOut,
            )
        }

        Text(
            stringResource(R.string.settings_footer),
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
                        contentDescription = stringResource(R.string.settings_profile_photo),
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
                    contentDescription = stringResource(R.string.settings_change_photo),
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
            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.settings_edit_name), tint = SyloBrandCyan, modifier = Modifier.size(18.dp))
        }
        Text(
            stringResource(R.string.settings_secured_locally),
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
        title = { Text(stringResource(R.string.settings_name_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.settings_enter_name)) },
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.settings_name_dialog_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** Dialog offering the three [ThemeMode] choices as radio options. */
@Composable
private fun ThemeDialog(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme)) },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(mode)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == selected, onClick = { onSelect(mode); onDismiss() })
                        Spacer(Modifier.width(SyloSpacing.stackSm))
                        Text(mode.label())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** Dialog offering System / English / Arabic as radio options for the app language. */
@Composable
private fun LanguageDialog(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        "" to stringResource(R.string.language_system),
        "en" to stringResource(R.string.language_english),
        "ar" to stringResource(R.string.language_arabic),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                options.forEach { (tag, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tag) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = tag == selected, onClick = { onSelect(tag) })
                        Spacer(Modifier.width(SyloSpacing.stackSm))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    },
)

@Composable
private fun languageLabel(tag: String): String = stringResource(
    when (tag) {
        "en" -> R.string.language_english
        "ar" -> R.string.language_arabic
        else -> R.string.language_system
    },
)

/** Unwraps the hosting [Activity] from a Compose [Context], or null if not found. */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
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
                Text(stringResource(R.string.settings_pro_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.settings_pro_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = RoundedCornerShape(50), color = SyloBrandCyan) {
                Text(
                    stringResource(R.string.settings_upgrade),
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
            StatCard(stringResource(R.string.settings_stat_total_saved), stats.totalBalance, Icons.Filled.AccountBalanceWallet, SyloIncomeGreen, Modifier.weight(1f))
            StatCard(stringResource(R.string.settings_stat_days_tracking), stats.daysTracking, Icons.Filled.CalendarMonth, SyloBrandCyan, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SyloSpacing.stackSm)) {
            StatCard(stringResource(R.string.settings_stat_transactions), stats.transactions, Icons.AutoMirrored.Filled.ReceiptLong, SyloBrandCyan, Modifier.weight(1f))
            StatCard(stringResource(R.string.settings_stat_this_month), stats.thisMonth, Icons.Filled.Savings, SyloBrandCyan, Modifier.weight(1f))
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
            stringResource(R.string.common_active),
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
            Text(stringResource(R.string.settings_verified_badge), style = MaterialTheme.typography.labelSmall, color = SyloBrandCyan)
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
        Toast.makeText(this, getString(R.string.common_no_browser), Toast.LENGTH_SHORT).show()
    }
}
