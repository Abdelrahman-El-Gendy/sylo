package com.sylo.feature.dashboard

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.core.ui.component.categoryIcon
import com.sylo.core.ui.component.formatMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import javax.inject.Inject

enum class AnalyticsPeriod(val label: String, val days: Int) {
    Week("Week", 7), Month("Month", 30), Year("Year", 365)
}

data class CategorySpend(
    val category: String,
    val amountLabel: String,
    val transactions: Int,
    val percent: Int,
    val icon: ImageVector,
)

data class AnalyticsUiState(
    val period: AnalyticsPeriod = AnalyticsPeriod.Week,
    val totalSpending: String = "$0.00",
    val trendPercent: String? = null,
    val trendDown: Boolean = true,
    val categories: List<CategorySpend> = emptyList(),
    val trend: List<Float> = emptyList(),
    val hasData: Boolean = false,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private var all: List<TransactionEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { entities ->
                all = entities
                recompute()
            }
        }
    }

    fun onPeriodChange(period: AnalyticsPeriod) {
        _uiState.update { it.copy(period = period) }
        recompute()
    }

    private fun recompute() {
        val period = _uiState.value.period
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        val windowMs = period.days * day
        val periodStart = now - windowMs

        // Expenses only (negative amounts) within the current window.
        val expenses = all.filter { it.amountMinor < 0 && it.timestampEpochMillis >= periodStart }
        val currency = all.firstOrNull()?.currency ?: "USD"
        val totalMinor = expenses.sumOf { abs(it.amountMinor) }

        // Previous equal-length window, for the trend comparison.
        val prevExpenses = all.filter {
            it.amountMinor < 0 &&
                it.timestampEpochMillis in (periodStart - windowMs) until periodStart
        }
        val prevMinor = prevExpenses.sumOf { abs(it.amountMinor) }
        val trend = when {
            prevMinor == 0L -> null
            else -> {
                val change = (totalMinor - prevMinor) * 100.0 / prevMinor
                "%+.1f%%".format(change)
            }
        }

        // Category breakdown.
        val categories = expenses
            .groupBy { it.category }
            .map { (cat, txs) ->
                val sum = txs.sumOf { abs(it.amountMinor) }
                CategorySpend(
                    category = cat,
                    amountLabel = formatMoney(sum, currency),
                    transactions = txs.size,
                    percent = if (totalMinor == 0L) 0 else (sum * 100.0 / totalMinor).roundToInt(),
                    icon = categoryIcon(cat),
                )
            }
            .sortedByDescending { it.percent }

        // Chart: bucket expenses into equal slices across the window, normalised 0..1.
        val buckets = period.days.coerceAtMost(12)
        val bucketMs = windowMs / buckets
        val sums = LongArray(buckets)
        expenses.forEach {
            val idx = ((it.timestampEpochMillis - periodStart) / bucketMs).toInt().coerceIn(0, buckets - 1)
            sums[idx] += abs(it.amountMinor)
        }
        val max = (sums.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val points = sums.map { (it.toFloat() / max).coerceIn(0f, 1f) }

        _uiState.update {
            it.copy(
                totalSpending = formatMoney(totalMinor, currency),
                trendPercent = trend,
                trendDown = (trend?.startsWith("-") == true),
                categories = categories,
                trend = points,
                hasData = expenses.isNotEmpty(),
            )
        }
    }
}
