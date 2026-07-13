package com.sylo.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.core.ui.component.UiTransaction
import com.sylo.core.ui.component.categoryIcon
import com.sylo.core.ui.component.formatAmountLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class HistoryFilter { Daily, Weekly, Monthly }

data class TransactionGroup(val label: String, val items: List<UiTransaction>)

data class HistoryUiState(
    val query: String = "",
    val filter: HistoryFilter = HistoryFilter.Daily,
    val groups: List<TransactionGroup> = emptyList(),
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var allTransactions: List<TransactionEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { entities ->
                allTransactions = entities
                rebuild()
            }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        rebuild()
    }

    fun onFilterChange(filter: HistoryFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    private fun rebuild() {
        val query = _uiState.value.query.trim().lowercase()
        val filtered = allTransactions.filter {
            query.isBlank() || it.title.lowercase().contains(query) || it.category.lowercase().contains(query)
        }
        val groups = filtered
            .groupBy { dayLabel(it.timestampEpochMillis) }
            .map { (label, items) -> TransactionGroup(label, items.map(::toUi)) }
        _uiState.update { it.copy(groups = groups) }
    }

    private fun toUi(e: TransactionEntity) = UiTransaction(
        id = e.id,
        merchant = e.title,
        subtitle = "${e.category} · ${timeLabel(e.timestampEpochMillis)}",
        amountLabel = formatAmountLabel(e.amountMinor, e.currency),
        icon = categoryIcon(e.category),
        isIncome = e.amountMinor > 0,
        status = e.status,
    )

    private fun timeLabel(epochMillis: Long): String =
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(epochMillis)

    private fun dayLabel(epochMillis: Long): String {
        val today = startOfDay(System.currentTimeMillis())
        val day = startOfDay(epochMillis)
        val dayMs = 24 * 60 * 60 * 1000L
        return when (today - day) {
            0L -> "Today"
            dayMs -> "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(epochMillis)
        }
    }

    private fun startOfDay(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
