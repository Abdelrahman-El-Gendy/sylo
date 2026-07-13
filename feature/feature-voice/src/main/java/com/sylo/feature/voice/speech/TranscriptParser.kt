package com.sylo.feature.voice.speech

/** Structured expense extracted from a spoken sentence like "add 45 dirhams for lunch". */
data class ParsedExpense(
    val amountMinor: Long,
    val currency: String,
    val category: String,
    val note: String,
) {
    val amountDisplay: String get() = "%.2f".format(amountMinor / 100.0)
}

/**
 * Very small rule-based parser: pulls the first number as the amount, guesses the
 * currency and category from keywords, and keeps the full utterance as the note.
 * A real build would swap this for an on-device NLU model.
 */
object TranscriptParser {

    private val amountRegex = Regex("""(\d+(?:[.,]\d{1,2})?)""")

    private val currencyKeywords = mapOf(
        "dirham" to "AED", "aed" to "AED", "dollar" to "USD", "buck" to "USD",
        "euro" to "EUR", "pound" to "GBP", "riyal" to "SAR",
    )

    private val categoryKeywords = mapOf(
        "Food" to listOf("lunch", "dinner", "breakfast", "coffee", "food", "cafe", "restaurant", "grocery", "groceries"),
        "Transport" to listOf("uber", "taxi", "cab", "fuel", "gas", "petrol", "bus", "train", "ride"),
        "Shopping" to listOf("shop", "clothes", "store", "amazon", "apple"),
        "Entertainment" to listOf("movie", "netflix", "game", "concert", "cinema"),
        "Bills" to listOf("bill", "rent", "electricity", "subscription", "internet"),
    )

    fun parse(transcript: String): ParsedExpense {
        val lower = transcript.lowercase()

        val amount = amountRegex.find(lower)?.groupValues?.get(1)
            ?.replace(',', '.')?.toDoubleOrNull() ?: 0.0

        val currency = currencyKeywords.entries.firstOrNull { lower.contains(it.key) }?.value ?: "AED"

        val category = categoryKeywords.entries
            .firstOrNull { (_, words) -> words.any { lower.contains(it) } }?.key ?: "General"

        return ParsedExpense(
            amountMinor = (amount * 100).toLong(),
            currency = currency,
            category = category,
            note = transcript.trim(),
        )
    }
}
