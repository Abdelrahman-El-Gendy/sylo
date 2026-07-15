package com.sylo.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.security.biometric.BiometricCipherResult
import com.sylo.core.ui.component.SyloLogo
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing
import com.sylo.feature.auth.ui.component.PinDots
import com.sylo.feature.auth.ui.component.PinKeypad

@Composable
fun PinSetupRoute(
    onPinCreated: () -> Unit,
    onClose: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // After the PIN is stored, optionally wrap the DB key to a biometric CryptoObject.
    fun finishSetup() {
        val cipher = viewModel.encryptCipherForBinding()
        val activity = context.findFragmentActivity()
        if (cipher != null && activity != null) {
            viewModel.biometricAuthenticator.authenticate(
                activity = activity,
                title = "Enable biometric unlock",
                subtitle = "Bind your encrypted data to your fingerprint",
                cipher = cipher,
            ) { result ->
                if (result is BiometricCipherResult.Success) viewModel.completeBinding(result.cipher)
                onPinCreated() // binding is best-effort; proceed regardless
            }
        } else {
            onPinCreated()
        }
    }

    PinSetupScreen(
        uiState = uiState,
        onDigit = { digit ->
            viewModel.onDigit(digit)
            // Hashing is async (PBKDF2 on a background thread) so the 4th dot gets
            // its frame to render before the flow moves on.
            if (viewModel.uiState.value.isComplete) viewModel.submitSetup(::finishSetup)
        },
        onBackspace = viewModel::onBackspace,
        onToggleBiometric = viewModel::toggleBiometric,
        onClose = onClose,
    )
}

@Composable
private fun PinSetupScreen(
    uiState: AuthUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onToggleBiometric: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SyloSpacing.containerMargin, vertical = SyloSpacing.stackMd),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SyloLogo()
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.size(SyloSpacing.stackLg))
        Text(
            "Create your secure PIN",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(SyloSpacing.stackSm))
        Text(
            "This PIN will be used to authorize transactions and secure your financial data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.size(SyloSpacing.stackLg))
        PinDots(filled = uiState.enteredDigits, total = PIN_LENGTH, isVerifying = uiState.isVerifying)

        Spacer(Modifier.weight(1f))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(SyloSpacing.stackMd)) {
                PinKeypad(onDigit = onDigit, onBackspace = onBackspace, enabled = !uiState.isVerifying)
                Spacer(Modifier.size(SyloSpacing.stackMd))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = SyloBrandCyan,
                    )
                    Spacer(Modifier.size(SyloSpacing.stackSm))
                    Text(
                        "Enable Biometric Login",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = uiState.biometricEnabled,
                        onCheckedChange = { onToggleBiometric() },
                    )
                }
            }
        }

        Spacer(Modifier.size(SyloSpacing.stackMd))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(SyloSpacing.unit))
            Text(
                "End-to-end encrypted storage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
