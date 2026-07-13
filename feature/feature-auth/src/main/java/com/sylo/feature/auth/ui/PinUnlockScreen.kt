package com.sylo.feature.auth.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.security.biometric.BiometricCipherResult
import com.sylo.core.security.biometric.BiometricResult
import com.sylo.core.ui.theme.SyloBrandCyan
import com.sylo.core.ui.theme.SyloSpacing
import com.sylo.feature.auth.ui.component.PinDots
import com.sylo.feature.auth.ui.component.PinKeypad

@Composable
fun PinUnlockRoute(
    onUnlocked: () -> Unit,
    onNeedSetup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // First launch (no PIN yet) -> send the user to create one.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!viewModel.hasPin()) onNeedSetup()
    }

    fun tryBiometric() {
        val activity = context.findFragmentActivity() ?: return
        val decryptCipher = viewModel.decryptCipherForUnlock()
        if (decryptCipher != null) {
            // DB key is biometric-bound: unlock via CryptoObject and release the key.
            viewModel.biometricAuthenticator.authenticate(
                activity = activity,
                title = "Unlock Sylo",
                subtitle = "Confirm your identity to decrypt your data",
                cipher = decryptCipher,
            ) { result ->
                when (result) {
                    is BiometricCipherResult.Success -> {
                        viewModel.completeCryptoUnlock(result.cipher)
                        onUnlocked()
                    }
                    is BiometricCipherResult.Error -> if (result.code !in CANCEL_CODES) viewModel.showBiometricError(result.message)
                    BiometricCipherResult.Failed -> Unit
                }
            }
        } else {
            viewModel.biometricAuthenticator.authenticate(
                activity = activity,
                title = "Unlock Sylo",
                subtitle = "Confirm your identity to unlock",
            ) { result ->
                when (result) {
                    is BiometricResult.Success -> onUnlocked()
                    is BiometricResult.Error -> if (result.code !in CANCEL_CODES) viewModel.showBiometricError(result.message)
                    BiometricResult.Failed -> Unit // not recognised — the prompt stays open
                }
            }
        }
    }

    // Evaluate biometric availability ONCE — canOfferBiometric() does a Binder IPC to
    // BiometricService, so calling it inline would fire on every recomposition (every
    // PIN keystroke), causing main-thread jank during entry.
    val showBiometric = androidx.compose.runtime.remember { viewModel.canOfferBiometric() }
    // Auto-present the biometric prompt once when it's available.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (showBiometric) tryBiometric()
    }

    PinUnlockScreen(
        uiState = uiState,
        showBiometric = showBiometric,
        onDigit = { digit ->
            viewModel.onDigit(digit)
            if (viewModel.uiState.value.isComplete && viewModel.verifyUnlock()) onUnlocked()
        },
        onBackspace = viewModel::onBackspace,
        onBiometric = ::tryBiometric,
    )
}

// BiometricPrompt error codes that mean "user backed out" — not real failures.
private val CANCEL_CODES = setOf(5, 10, 13) // ERROR_CANCELED, ERROR_USER_CANCELED, ERROR_NEGATIVE_BUTTON

@Composable
private fun PinUnlockScreen(
    uiState: AuthUiState,
    showBiometric: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SyloSpacing.containerMargin, vertical = SyloSpacing.stackLg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.4f))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = SyloBrandCyan,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Spacer(Modifier.size(SyloSpacing.stackLg))
        Text("Welcome Back", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(SyloSpacing.stackSm))
        Text(
            "Enter PIN to unlock Sylo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.size(SyloSpacing.stackLg))
        PinDots(filled = uiState.enteredDigits, total = PIN_LENGTH)
        // Fixed-height slot so showing/clearing the error never shifts the layout.
        Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) {
            uiState.error?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.weight(0.6f))
        PinKeypad(onDigit = onDigit, onBackspace = onBackspace)

        Spacer(Modifier.size(SyloSpacing.stackMd))
        if (showBiometric) {
            TextButton(onClick = onBiometric) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = SyloBrandCyan)
                Spacer(Modifier.size(SyloSpacing.stackSm))
                Text(
                    "UNLOCK WITH BIOMETRICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SyloBrandCyan,
                )
            }
        }

        Spacer(Modifier.size(SyloSpacing.stackSm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                "Forgot PIN?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                "Emergency",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
