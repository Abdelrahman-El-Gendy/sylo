package com.sylo.feature.auth.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.core.security.biometric.BiometricCipherResult
import com.sylo.core.security.biometric.BiometricResult
import com.sylo.core.ui.R
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
    val bioTitle = stringResource(R.string.bio_unlock_title)
    val bioSubtitleDecrypt = stringResource(R.string.bio_unlock_subtitle_decrypt)
    val bioSubtitle = stringResource(R.string.bio_unlock_subtitle)

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
                title = bioTitle,
                subtitle = bioSubtitleDecrypt,
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
                title = bioTitle,
                subtitle = bioSubtitle,
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
            // Verification is async (PBKDF2 on a background thread) so the 4th dot
            // gets its frame to render before any navigation happens.
            if (viewModel.uiState.value.isComplete) viewModel.submitUnlock(onUnlocked)
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
        Text(stringResource(R.string.unlock_welcome), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(SyloSpacing.stackSm))
        Text(
            stringResource(if (uiState.isVerifying) R.string.unlock_verifying else R.string.unlock_enter_pin),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.size(SyloSpacing.stackLg))
        PinDots(
            filled = uiState.enteredDigits,
            total = PIN_LENGTH,
            isError = uiState.error != null,
            isVerifying = uiState.isVerifying,
        )
        // Fixed-height slot so showing/clearing the error never shifts the layout.
        Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) {
            // Keep the last message around so the exit fade doesn't show an empty label.
            var lastError by remember { mutableStateOf("") }
            uiState.error?.let { lastError = it }
            // Fully qualified: inside Box-within-Column, plain AnimatedVisibility resolves
            // to the ColumnScope extension, which can't be called on the Box receiver.
            androidx.compose.animation.AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut(),
            ) {
                Text(lastError, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.weight(0.6f))
        PinKeypad(onDigit = onDigit, onBackspace = onBackspace, enabled = !uiState.isVerifying)

        Spacer(Modifier.size(SyloSpacing.stackMd))
        if (showBiometric) {
            TextButton(onClick = onBiometric) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = SyloBrandCyan)
                Spacer(Modifier.size(SyloSpacing.stackSm))
                Text(
                    stringResource(R.string.unlock_biometrics),
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
                stringResource(R.string.unlock_forgot_pin),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.unlock_emergency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
