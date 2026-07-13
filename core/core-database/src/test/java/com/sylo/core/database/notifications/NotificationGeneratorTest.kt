package com.sylo.core.database.notifications

import com.sylo.core.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationGeneratorTest {

    private val now = 1_700_000_000_000L // fixed "now"
    private val hour = 60 * 60 * 1000L

    private fun tx(
        id: String,
        minor: Long,
        category: String,
        title: String,
        offsetMs: Long,
        status: String? = null,
    ) =
        TransactionEntity(
            id = id, title = title, amountMinor = minor, currency = "USD",
            category = category, note = null, status = status,
            timestampEpochMillis = now - offsetMs,
        )

    @Test
    fun empty_transactions_yield_no_notifications() {
        assertTrue(NotificationGenerator.generate(emptyList(), now).isEmpty())
    }

    @Test
    fun large_expense_and_income_and_insight_are_derived() {
        val txns = listOf(
            tx("t1", -25000, "Electronics", "Apple Store", 2 * hour),   // large expense
            tx("t2", 450000, "Income", "Salary", 3 * hour),             // income
            tx("t3", -500, "Food & Drinks", "Coffee", 4 * hour),        // small expense (no large alert)
        )
        val result = NotificationGenerator.generate(txns, now)
        val ids = result.map { it.id }

        assertTrue("large expense alert", ids.contains("large-t1"))
        assertTrue("income alert", ids.contains("income-t2"))
        assertTrue("no large alert for small expense", !ids.contains("large-t3"))
        assertTrue("top-category insight present", ids.any { it.startsWith("insight-topcat-") })
        // Deterministic ids -> stable across regenerations
        assertEquals(result.map { it.id }, NotificationGenerator.generate(txns, now).map { it.id })
    }

    @Test
    fun auto_captured_transactions_get_a_captured_alert_not_large_or_income() {
        val txns = listOf(
            tx("s1", -25000, "Groceries", "Carrefour", 1 * hour, status = "SMS"),   // large, but auto
            tx("a1", 300000, "Income", "Employer", 2 * hour, status = "Auto"),        // income, but auto
        )
        val ids = NotificationGenerator.generate(txns, now).map { it.id }

        assertTrue("SMS capture alert", ids.contains("captured-s1"))
        assertTrue("notification capture alert", ids.contains("captured-a1"))
        // Auto captures must NOT also produce large/income alerts (no duplicates).
        assertTrue("no duplicate large alert", !ids.contains("large-s1"))
        assertTrue("no duplicate income alert", !ids.contains("income-a1"))
    }
}
