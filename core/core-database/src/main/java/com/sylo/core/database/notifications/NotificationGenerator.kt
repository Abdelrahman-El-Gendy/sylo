package com.sylo.core.database.notifications

import com.sylo.core.database.entity.TransactionEntity
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Derives notifications from the user's actual transactions — no hardcoded feed.
 * IDs are deterministic (stable across regenerations) so read/cleared state persists.
 */
object NotificationGenerator {

    /** Single expenses at/above this magnitude (minor units) trigger a large-expense alert. */
    private const val LARGE_EXPENSE_MINOR = 20_000L // e.g. $200.00
    private const val DAY = 24 * 60 * 60 * 1000L

    /** Transaction statuses written by the auto-capture paths (notification listener + SMS). */
    private val AUTO_STATUSES = setOf("Auto", "SMS")

    fun generate(transactions: List<TransactionEntity>, nowMillis: Long): List<SyloNotification> {
        if (transactions.isEmpty()) return emptyList()
        val out = mutableListOf<SyloNotification>()
        val weekAgo = nowMillis - 7 * DAY
        val monthStart = startOfMonth(nowMillis)

        // Auto-captured payments (bank notification / SMS) get their own alert so every
        // capture surfaces in the feed — not just the large ones. These are excluded
        // from the large/income alerts below to avoid a duplicate entry.
        transactions
            .filter { it.status in AUTO_STATUSES && it.timestampEpochMillis >= weekAgo }
            .forEach { tx ->
                val income = tx.amountMinor > 0
                out += SyloNotification(
                    id = "captured-${tx.id}",
                    kind = NotificationKind.CAPTURED,
                    title = if (income) "Payment received" else "Expense captured",
                    body = if (income) {
                        "Auto-logged ${money(tx.amountMinor, tx.currency)} — ${tx.title}."
                    } else {
                        "Auto-logged ${money(tx.amountMinor, tx.currency)} at ${tx.title}."
                    },
                    timestampEpochMillis = tx.timestampEpochMillis,
                )
            }

        // Recent large expenses (manual/voice entries only — auto captures covered above)
        transactions
            .filter {
                it.status !in AUTO_STATUSES &&
                    it.amountMinor <= -LARGE_EXPENSE_MINOR && it.timestampEpochMillis >= weekAgo
            }
            .forEach { tx ->
                out += SyloNotification(
                    id = "large-${tx.id}",
                    kind = NotificationKind.LARGE_EXPENSE,
                    title = "Large expense",
                    body = "You spent ${money(tx.amountMinor, tx.currency)} at ${tx.title}.",
                    timestampEpochMillis = tx.timestampEpochMillis,
                )
            }

        // Recent income (manual/voice entries only)
        transactions
            .filter { it.status !in AUTO_STATUSES && it.amountMinor > 0 && it.timestampEpochMillis >= weekAgo }
            .forEach { tx ->
                out += SyloNotification(
                    id = "income-${tx.id}",
                    kind = NotificationKind.INCOME,
                    title = "Payment received",
                    body = "You received ${money(tx.amountMinor, tx.currency)} — ${tx.title}.",
                    timestampEpochMillis = tx.timestampEpochMillis,
                )
            }

        // Top spending category this month (insight)
        val monthExpenses = transactions.filter { it.amountMinor < 0 && it.timestampEpochMillis >= monthStart }
        if (monthExpenses.isNotEmpty()) {
            val total = monthExpenses.sumOf { abs(it.amountMinor) }
            val top = monthExpenses.groupBy { it.category }
                .mapValues { (_, txs) -> txs.sumOf { abs(it.amountMinor) } }
                .maxByOrNull { it.value }
            if (top != null && total > 0) {
                val pct = (top.value * 100.0 / total).roundToInt()
                out += SyloNotification(
                    id = "insight-topcat-${yearMonth(nowMillis)}",
                    kind = NotificationKind.INSIGHT,
                    title = "Sylo Insight",
                    body = "${top.key} is your top spending category this month ($pct%).",
                    timestampEpochMillis = nowMillis,
                )
            }

            // Weekly spending summary
            val weekExpenses = monthExpenses.filter { it.timestampEpochMillis >= weekAgo }
            if (weekExpenses.isNotEmpty()) {
                val weekTotal = weekExpenses.sumOf { abs(it.amountMinor) }
                val currency = weekExpenses.first().currency
                out += SyloNotification(
                    id = "summary-week-${weekOfYear(nowMillis)}",
                    kind = NotificationKind.SUMMARY,
                    title = "This week's spending",
                    body = "You spent ${money(-weekTotal, currency)} across ${weekExpenses.size} transactions this week.",
                    timestampEpochMillis = nowMillis,
                )
            }
        }

        return out.sortedByDescending { it.timestampEpochMillis }
    }

    private fun money(amountMinor: Long, currency: String): String {
        val whole = abs(amountMinor) / 100
        val cents = abs(amountMinor) % 100
        val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
        val n = "%s.%02d".format(grouped, cents)
        return if (currency == "USD") "\$$n" else "$n $currency"
    }

    private fun cal(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }
    private fun startOfMonth(millis: Long) = cal(millis).apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private fun yearMonth(millis: Long) = cal(millis).let { "${it.get(Calendar.YEAR)}${it.get(Calendar.MONTH)}" }
    private fun weekOfYear(millis: Long) = cal(millis).let { "${it.get(Calendar.YEAR)}${it.get(Calendar.WEEK_OF_YEAR)}" }
}
