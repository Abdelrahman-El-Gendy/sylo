package com.sylo.core.common.payments

/** A payment extracted from a bank / wallet notification. Amount is a positive magnitude. */
data class ParsedPayment(
    val amountMinor: Long,
    val currency: String,
    val merchant: String,
    val category: String,
    val isIncome: Boolean,
    val sourceApp: String,
    val rawText: String,
)

/**
 * Heuristic parser that turns a bank/wallet notification into a [ParsedPayment].
 *
 * A notification is treated as a payment when it contains a currency amount AND
 * either comes from a known payment app or contains a spend/receive keyword. This
 * keeps noise out while still working for arbitrary bank apps. Returns null when the
 * notification isn't a payment.
 */
object PaymentNotificationParser {

    /** package-name fragment -> friendly source name. Extend as new apps are supported. */
    private val KNOWN_APPS = mapOf(
        "instapay" to "InstaPay",
        "com.bankaudi" to "Bank Audi",
        "com.cib" to "CIB",
        "nbe" to "NBE",
        "qnb" to "QNB",
        "com.google.android.apps.walletnfcrel" to "Google Wallet",
        "com.paypal" to "PayPal",
        "vodafonecash" to "Vodafone Cash",
    )

    private val expenseKeywords = listOf(
        "spent", "debited", "debit", "paid", "payment", "purchase", "withdrawn",
        "withdrawal", "sent", "deducted", "charged", "pos ",
    )
    private val incomeKeywords = listOf("received", "credited", "credit", "refund", "deposit", "salary")

    /**
     * Messages that describe settling/paying off a card or bill between the user's own
     * accounts. The purchases they cover were already captured individually, so
     * recording the settlement too would double-count the same spend.
     * ("settl" covers settle / settled / settlement / settling.)
     */
    private val transferKeywords = listOf(
        "settl", "credit card bill", "card bill", "installment", "instalment", "repayment",
    )

    private val categoryKeywords = mapOf(
        "Groceries" to listOf("carrefour", "grocery", "market", "spinneys", "supermarket"),
        "Transport" to listOf("uber", "careem", "fuel", "petrol", "taxi", "metro"),
        "Food & Drinks" to listOf("restaurant", "cafe", "coffee", "starbucks", "mcdonald", "kfc"),
        "Shopping" to listOf("amazon", "noon", "mall", "store", "shop"),
        "Bills" to listOf("electricity", "utility", "vodafone", "orange", "etisalat", "subscription"),
    )

    // Amount as "EGP 250.00" / "$12.99" or "250.00 EGP" / "45 dollars".
    private val amountRegex = Regex(
        """(?i)(?:(EGP|AED|USD|SAR|GBP|EUR|\$|£|€)\s?([0-9][0-9,]*(?:\.[0-9]{1,2})?))""" +
            """|(?:([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s?(EGP|AED|USD|SAR|GBP|EUR|pounds?|dollars?|dirhams?))""",
    )

    private val merchantRegex =
        Regex("""(?i)\b(?:at|to|from|@)\s+([A-Za-z0-9][A-Za-z0-9 &'.\-]{1,30})""")

    private val currencyNames = mapOf(
        "$" to "USD", "£" to "GBP", "€" to "EUR",
        "pound" to "GBP", "pounds" to "GBP", "dollar" to "USD", "dollars" to "USD", "dirham" to "AED", "dirhams" to "AED",
    )

    fun parse(packageName: String, title: String?, text: String?): ParsedPayment? {
        val content = listOfNotNull(title, text).joinToString(" ").trim()
        if (content.isBlank()) return null
        val lower = content.lowercase()

        // Card/bill settlements are transfers between the user's own accounts,
        // not new spending — skip them entirely.
        if (transferKeywords.any { lower.contains(it) }) return null

        val match = amountRegex.find(content) ?: return null

        val knownApp = KNOWN_APPS.entries.firstOrNull { packageName.contains(it.key, ignoreCase = true) }
        val hasExpense = expenseKeywords.any { lower.contains(it) }
        val hasIncome = incomeKeywords.any { lower.contains(it) }
        if (knownApp == null && !hasExpense && !hasIncome) return null

        val (rawCurrency, rawAmount) = extractAmount(match) ?: return null
        val amount = rawAmount.replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val currency = normaliseCurrency(rawCurrency)
        val isIncome = hasIncome && !hasExpense
        val merchant = merchantRegex.find(content)?.groupValues?.get(1)?.let(::cleanMerchant)
            ?: knownApp?.value
            ?: title?.takeIf { it.isNotBlank() }
            ?: "Payment"
        val category = guessCategory(lower)

        return ParsedPayment(
            amountMinor = (amount * 100).toLong(),
            currency = currency,
            merchant = merchant,
            category = category,
            isIncome = isIncome,
            sourceApp = knownApp?.value ?: "Notification",
            rawText = content,
        )
    }

    private fun extractAmount(m: MatchResult): Pair<String, String>? {
        val g = m.groupValues
        return when {
            g[2].isNotBlank() -> g[1] to g[2] // symbol/code before number
            g[3].isNotBlank() -> g[4] to g[3] // number before code/name
            else -> null
        }
    }

    private fun normaliseCurrency(raw: String): String {
        val key = raw.lowercase()
        currencyNames[raw]?.let { return it }
        currencyNames[key]?.let { return it }
        return raw.uppercase().takeIf { it.length == 3 } ?: "USD"
    }

    private val merchantStopWords = (expenseKeywords + incomeKeywords + listOf(
        "on", "for", "via", "your", "account", "card", "using", "with", "at", "to", "from",
    )).map { it.trim() }.toSet()

    /** Keeps merchant words up to the first trailing keyword, e.g. "Netflix sent" -> "Netflix". */
    private fun cleanMerchant(raw: String): String? {
        val cleaned = raw.trim().split(Regex("\\s+"))
            .takeWhile { it.lowercase().trimEnd('.', ',') !in merchantStopWords }
            .joinToString(" ")
            .trim(' ', '.', ',', '-')
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun guessCategory(lower: String): String =
        categoryKeywords.entries.firstOrNull { (_, words) -> words.any { lower.contains(it) } }?.key ?: "Payments"
}
