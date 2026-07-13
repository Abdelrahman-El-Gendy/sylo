package com.sylo.core.ui.component

/**
 * Maps an ISO currency code to the glyph shown in front of an amount. Currencies
 * without a common single-glyph symbol fall back to the code itself (e.g. "EGP"),
 * so the amount always carries a clear currency indicator.
 */
fun currencySymbol(code: String): String = when (code) {
    "USD" -> "$"
    "GBP" -> "£"
    "EUR" -> "€"
    "JPY" -> "¥"
    else -> code
}
