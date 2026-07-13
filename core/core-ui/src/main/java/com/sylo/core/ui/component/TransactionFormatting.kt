package com.sylo.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/** Maps a category string to its icon — shared so dashboard + history render alike. */
fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "groceries" -> Icons.Filled.ShoppingCart
    "transport" -> Icons.Filled.DirectionsCar
    "entertainment" -> Icons.Filled.Movie
    "electronics", "shopping" -> Icons.Filled.ShoppingBag
    "income" -> Icons.Filled.Payments
    "food", "food & drinks" -> Icons.Filled.Restaurant
    "coffee" -> Icons.Filled.LocalCafe
    else -> Icons.AutoMirrored.Filled.ReceiptLong
}

/**
 * Formats a signed minor-unit amount into a display label, e.g. -8420/USD -> "-$84.20",
 * 450000/USD -> "+$4,500.00", -4500/AED -> "-45.00 AED".
 */
fun formatAmountLabel(amountMinor: Long, currency: String): String {
    val sign = if (amountMinor > 0) "+" else "-"
    return sign + formatMoney(amountMinor, currency)
}

/** Unsigned money label, e.g. -8420/USD or 8420/USD -> "$84.20", 45200/AED -> "452.00 AED". */
fun formatMoney(amountMinor: Long, currency: String): String {
    val whole = kotlin.math.abs(amountMinor) / 100
    val cents = kotlin.math.abs(amountMinor) % 100
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    val number = "%s.%02d".format(grouped, cents)
    return if (currency == "USD") "\$$number" else "$number $currency"
}
