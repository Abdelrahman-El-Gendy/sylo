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
        "dirham" to "AED", "aed" to "AED", "درهم" to "AED", "دراهم" to "AED",
        "dollar" to "USD", "buck" to "USD", "دولار" to "USD",
        "euro" to "EUR", "يورو" to "EUR",
        "pound" to "GBP", "جنيه" to "GBP",
        "riyal" to "SAR", "ريال" to "SAR", "ريالات" to "SAR",
    )

    private val categoryKeywords = mapOf(
        "Food" to listOf(
            "lunch", "dinner", "breakfast", "coffee", "food", "cafe", "restaurant", "grocery", "groceries",
            "غداء", "غدا", "عشاء", "فطور", "فطار", "قهوة", "مطعم", "بقالة", "سوبرماركت",
        ),
        "Transport" to listOf(
            "uber", "taxi", "cab", "fuel", "gas", "petrol", "bus", "train", "ride",
            "أوبر", "تاكسي", "تكسي", "وقود", "بنزين", "باص", "حافلة", "قطار",
        ),
        "Shopping" to listOf(
            "shop", "clothes", "store", "amazon", "apple",
            "تسوق", "ملابس", "متجر", "محل", "أمازون",
        ),
        "Entertainment" to listOf(
            "movie", "netflix", "game", "concert", "cinema",
            "فيلم", "سينما", "نتفليكس", "لعبة", "حفلة",
        ),
        "Bills" to listOf(
            "bill", "rent", "electricity", "subscription", "internet",
            "فاتورة", "إيجار", "كهرباء", "اشتراك", "إنترنت",
        ),
    )

    /**
     * Speech recognition in Arabic can return Eastern Arabic-Indic digits (٠-٩) or
     * Persian ones (۰-۹), plus the Arabic decimal/thousands separators, instead of
     * ASCII. Normalize them so [amountRegex] (which only matches ASCII digits) works
     * regardless of dictation language.
     */
    private fun normalizeDigits(text: String): String = buildString(text.length) {
        for (ch in text) {
            append(
                when (ch) {
                    in '٠'..'٩' -> '0' + (ch - '٠') // Eastern Arabic-Indic
                    in '۰'..'۹' -> '0' + (ch - '۰') // Persian
                    '٫' -> '.' // Arabic decimal separator
                    '٬' -> ',' // Arabic thousands separator
                    else -> ch
                },
            )
        }
    }

    fun parse(transcript: String): ParsedExpense {
        val lower = normalizeDigits(transcript).lowercase()

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
