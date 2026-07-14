package com.sylo.com.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sylo.com.MainActivity
import com.sylo.com.R
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.database.entity.TransactionEntity
import com.sylo.core.ui.component.formatAmountLabel
import com.sylo.core.ui.component.formatMoney
import java.text.SimpleDateFormat
import java.util.Locale

private val WidgetBackground = ColorProvider(Color(0xFF1D2022))
private val WidgetOnSurface = ColorProvider(Color(0xFFE0E3E5))
private val WidgetOnSurfaceVariant = ColorProvider(Color(0xFFB9CACB))
private val WidgetHairline = ColorProvider(Color(0x33B9CACB))
private val WidgetBrandCyan = ColorProvider(Color(0xFF00DBE9))
private val WidgetIncomeGreen = ColorProvider(Color(0xFF5DD6A0))

/**
 * Home-screen widget showing the current balance and the most recent transactions.
 * Reads through the same [com.sylo.core.database.TransactionRepository] /
 * [UserPreferencesRepository] the in-app Dashboard uses, via [widgetEntryPoint] since
 * Glance instantiates this class directly rather than through Hilt.
 *
 * Responsive: balance-only when short; two or three transaction rows as it grows.
 */
class SyloBalanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SIZE_BALANCE_ONLY, SIZE_TWO_ROWS, SIZE_THREE_ROWS),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val repository = entryPoint.transactionRepository()
        val userPreferences = entryPoint.userPreferencesRepository()

        provideContent {
            val transactions by repository.observeAll().collectAsState(initial = emptyList())
            val openingBalanceMinor by userPreferences.openingBalanceMinor.collectAsState(initial = 0L)
            val currency by userPreferences.currency.collectAsState(
                initial = UserPreferencesRepository.DEFAULT_CURRENCY,
            )

            GlanceTheme {
                WidgetContent(
                    balanceMinor = openingBalanceMinor + transactions.sumOf { it.amountMinor },
                    currency = currency,
                    transactions = transactions,
                )
            }
        }
    }

    companion object {
        private val SIZE_BALANCE_ONLY = DpSize(180.dp, 110.dp)
        private val SIZE_TWO_ROWS = DpSize(180.dp, 150.dp)
        private val SIZE_THREE_ROWS = DpSize(180.dp, 200.dp)

        /** How many transaction rows fit the granted height. */
        fun rowsFor(size: DpSize): Int = when {
            size.height >= SIZE_THREE_ROWS.height -> 3
            size.height >= SIZE_TWO_ROWS.height -> 2
            else -> 0
        }
    }
}

@Composable
private fun WidgetContent(balanceMinor: Long, currency: String, transactions: List<TransactionEntity>) {
    val context = LocalContext.current
    val rows = SyloBalanceWidget.rowsFor(LocalSize.current)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.ic_sylo_mark),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = "Sylo",
                style = TextStyle(color = WidgetOnSurfaceVariant, fontWeight = FontWeight.Medium, fontSize = 13.sp),
            )
        }

        Spacer(modifier = GlanceModifier.height(10.dp))
        Text(
            text = "Balance",
            style = TextStyle(color = WidgetOnSurfaceVariant, fontSize = 11.sp),
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = formatMoney(balanceMinor.coerceAtLeast(0L), currency),
            style = TextStyle(color = WidgetOnSurface, fontWeight = FontWeight.Bold, fontSize = 26.sp),
        )

        if (rows > 0) {
            Spacer(modifier = GlanceModifier.height(12.dp))
            Spacer(
                modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(WidgetHairline),
            )
            Spacer(modifier = GlanceModifier.height(10.dp))

            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions yet — add your first expense.",
                    style = TextStyle(color = WidgetOnSurfaceVariant, fontSize = 12.sp),
                )
            } else {
                transactions.take(rows).forEachIndexed { index, tx ->
                    if (index > 0) Spacer(modifier = GlanceModifier.height(8.dp))
                    TransactionRow(tx, currency)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity, fallbackCurrency: String) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = transaction.title,
                style = TextStyle(color = WidgetOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Text(
                text = transaction.category,
                style = TextStyle(color = WidgetOnSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(horizontalAlignment = Alignment.Horizontal.End) {
            Text(
                text = formatAmountLabel(transaction.amountMinor, transaction.currency.ifBlank { fallbackCurrency }),
                style = TextStyle(
                    color = if (transaction.amountMinor > 0) WidgetIncomeGreen else WidgetBrandCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
            )
            Text(
                text = dayLabel(transaction.timestampEpochMillis),
                style = TextStyle(color = WidgetOnSurfaceVariant, fontSize = 10.sp),
            )
        }
    }
}

/** "Today", "Yesterday", or a short date like "Jul 10". */
private fun dayLabel(epochMillis: Long): String = when {
    DateUtils.isToday(epochMillis) -> "Today"
    DateUtils.isToday(epochMillis + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(epochMillis)
}
