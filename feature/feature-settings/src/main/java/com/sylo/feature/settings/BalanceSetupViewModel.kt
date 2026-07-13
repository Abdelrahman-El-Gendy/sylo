package com.sylo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BalanceUiState(
    val amount: String = "",
    val currency: String = UserPreferencesRepository.DEFAULT_CURRENCY,
) {
    /** A balance is valid once the entered amount parses to a strictly positive number. */
    val isValid: Boolean get() = (amount.toDoubleOrNull() ?: 0.0) > 0.0
}

@HiltViewModel
class BalanceSetupViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    val currencies = UserPreferencesRepository.CURRENCIES

    private val _uiState = MutableStateFlow(BalanceUiState())
    val uiState: StateFlow<BalanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val opening = preferences.openingBalanceMinor.first()
            val currency = preferences.currency.first()
            _uiState.update {
                it.copy(
                    amount = if (opening != 0L) "%.2f".format(opening / 100.0) else "",
                    currency = currency,
                )
            }
        }
    }

    fun onAmountChange(value: String) {
        _uiState.update { it.copy(amount = value.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun onCurrencyChange(code: String) = _uiState.update { it.copy(currency = code) }

    fun save(onSaved: () -> Unit) {
        if (!_uiState.value.isValid) return
        viewModelScope.launch {
            val minor = ((_uiState.value.amount.toDoubleOrNull() ?: 0.0) * 100).toLong()
            preferences.setOpeningBalance(minor)
            preferences.setCurrency(_uiState.value.currency)
            // Marks the first-run balance step complete; harmless when editing later.
            preferences.setBalanceConfigured(true)
            onSaved()
        }
    }
}
