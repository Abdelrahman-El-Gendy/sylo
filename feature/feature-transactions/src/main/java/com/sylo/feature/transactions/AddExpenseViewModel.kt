package com.sylo.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.database.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val userPreferences: UserPreferencesRepository,
) : ViewModel() {

    /** The user's display currency, so the amount field can show the right symbol. */
    val currency: StateFlow<String> = userPreferences.currency
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferencesRepository.DEFAULT_CURRENCY)

    fun save(
        amount: String,
        category: String,
        note: String,
        receiptPath: String?,
        onSaved: () -> Unit,
    ) {
        val value = amount.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            repository.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    title = note.ifBlank { category },
                    amountMinor = -((value * 100).toLong()),
                    currency = userPreferences.currency.first(),
                    category = category,
                    note = note.ifBlank { null },
                    status = "Approved",
                    timestampEpochMillis = System.currentTimeMillis(),
                    receiptPath = receiptPath,
                )
            )
            onSaved()
        }
    }
}
