package com.sylo.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.core.ui.component.UiTransaction
import com.sylo.core.ui.component.categoryIcon
import com.sylo.core.ui.component.formatAmountLabel
import com.sylo.core.ui.component.formatMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt
import javax.inject.Inject

data class DashboardUiState(
    // Static: the balance the user set in Balance & Currency (not reduced by transactions).
    val totalBalance: String = "$0.00",
    // What's left after this month's withdrawals — shown beside Total inside the ring.
    val remainingBalance: String = "$0.00",
    // Amount withdrawn (spent) this month — shown below the ring.
    val withdrawals: String = "$0.00",
    // Fraction (0..1) of the total balance still remaining — drives the ring arc + water level.
    val remainingFraction: Float = 0f,
    // Signed net change this month, as a % of the total balance (the ring pill).
    val changePercent: String? = null,
    val changePositive: Boolean = true,
    val monthSpend: String = "$0.00",
    val recentTransactions: List<UiTransaction> = emptyList(),
    val spendTrend: List<Float> = emptyList(),
    val topCategory: String? = null,
    val topCategoryPercent: Int = 0,
    val hasData: Boolean = false,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val userPreferences: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeAll(),
                userPreferences.openingBalanceMinor,
                userPreferences.currency,
            ) { txns, openingBalance, currency ->
                Triple(txns, openingBalance, currency)
            }.collect { (txns, openingBalance, currency) ->
                render(txns, openingBalance, currency)
            }
        }
    }

    private fun render(all: List<TransactionEntity>, openingBalanceMinor: Long, currency: String) {
        val monthStart = startOfMonth()
        val monthTx = all.filter { it.timestampEpochMillis >= monthStart }
        val monthExpenses = monthTx.filter { it.amountMinor < 0 }
        val monthSpendMinor = monthExpenses.sumOf { abs(it.amountMinor) }
        val monthNetMinor = monthTx.sumOf { it.amountMinor }

        // Remaining = the (static) total balance adjusted by the net of ALL transactions.
        // Ring arc + water level = share of the total balance still remaining.
        val remainingMinor = openingBalanceMinor + all.sumOf { it.amountMinor }
        val remainingFraction = if (openingBalanceMinor > 0L) {
            (remainingMinor.toFloat() / openingBalanceMinor.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        // Pill: net change this month as a signed % of the total balance.
        val changePct = if (openingBalanceMinor > 0L) monthNetMinor * 100.0 / openingBalanceMinor else null

        // Simple, real insight: which category dominates this month's spend.
        val byCategory = monthExpenses.groupBy { it.category }
            .mapValues { (_, txs) -> txs.sumOf { abs(it.amountMinor) } }
        val top = byCategory.maxByOrNull { it.value }
        val topPercent = if (monthSpendMinor == 0L || top == null) 0
        else (top.value * 100.0 / monthSpendMinor).roundToInt()

        _uiState.update {
            it.copy(
                totalBalance = formatMoney(openingBalanceMinor, currency),
                remainingBalance = formatMoney(remainingMinor.coerceAtLeast(0L), currency),
                withdrawals = formatMoney(monthSpendMinor, currency),
                remainingFraction = remainingFraction,
                changePercent = changePct?.let { pct -> "%+.1f%%".format(pct) },
                changePositive = (changePct ?: 0.0) >= 0.0,
                monthSpend = formatMoney(monthSpendMinor, currency),
                recentTransactions = all.take(4).map(::toUi),
                spendTrend = dailyTrend(monthExpenses, monthStart),
                topCategory = top?.key,
                topCategoryPercent = topPercent,
                hasData = all.isNotEmpty() || openingBalanceMinor != 0L,
            )
        }
    }

    private fun dailyTrend(expenses: List<TransactionEntity>, monthStart: Long): List<Float> {
        if (expenses.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val buckets = 8
        val span = (now - monthStart).coerceAtLeast(1)
        val sums = LongArray(buckets)
        expenses.forEach {
            val idx = (((it.timestampEpochMillis - monthStart).toDouble() / span) * (buckets - 1)).toInt().coerceIn(0, buckets - 1)
            sums[idx] += abs(it.amountMinor)
        }
        val max = (sums.maxOrNull() ?: 0L).coerceAtLeast(1L)
        return sums.map { (it.toFloat() / max).coerceIn(0f, 1f) }
    }

    private fun startOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun toUi(e: TransactionEntity) = UiTransaction(
        id = e.id,
        merchant = e.title,
        subtitle = e.category,
        amountLabel = formatAmountLabel(e.amountMinor, e.currency),
        icon = categoryIcon(e.category),
        isIncome = e.amountMinor > 0,
    )
}
