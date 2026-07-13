package com.sylo.com.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sylo.core.common.payments.PaymentNotificationParser
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.database.entity.TransactionEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.math.abs

/**
 * Periodic (and on-demand) job that reads new bank-statement SMS for the current day
 * and records them as expenses — the interval-based sibling of the real-time
 * [com.sylo.com.notifications.PaymentNotificationListenerService].
 *
 * Everything it writes goes through [TransactionRepository], so captured SMS flow into
 * the dashboard, history, analytics, and the in-app notifications feed exactly like a
 * manual or voice entry. Deduplication is by a persisted high-water mark
 * ([UserPreferencesRepository.smsLastProcessedMillis]) so repeated intervals never
 * re-import the same message.
 */
@HiltWorker
class BankSmsScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val scanner: BankSmsScanner,
    private val transactionRepository: TransactionRepository,
    private val userPreferences: UserPreferencesRepository,
    private val notifier: BankSmsCaptureNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Respect the toggle even if a stale periodic run fires after it was disabled.
        if (!userPreferences.smsAutoCaptureEnabled.first()) return Result.success()

        val now = System.currentTimeMillis()
        val since = userPreferences.smsLastProcessedMillis.first()
        val messages = scanner.readTodaysMessagesSince(since, now)
        if (messages.isEmpty()) return Result.success()

        var highWaterMark = since
        var capturedCount = 0
        var latestSummary = ""

        for (sms in messages) {
            highWaterMark = maxOf(highWaterMark, sms.dateMillis)

            // Reuse the shared heuristic parser (also used by the notification listener).
            val parsed = PaymentNotificationParser.parse(sms.address, title = null, text = sms.body)
                ?: continue

            val signedMinor = if (parsed.isIncome) parsed.amountMinor else -parsed.amountMinor
            transactionRepository.add(
                TransactionEntity(
                    // Deterministic id derived from the message itself: if two scans race
                    // (e.g. the periodic's first run + the immediate one-shot) or a later
                    // rescan re-reads the same SMS, the upsert REPLACEs the same row rather
                    // than creating a duplicate expense.
                    id = smsTransactionId(sms),
                    title = parsed.merchant,
                    amountMinor = signedMinor,
                    currency = parsed.currency,
                    category = parsed.category,
                    note = "SMS ${sms.address}: ${parsed.rawText}",
                    status = "SMS",
                    timestampEpochMillis = sms.dateMillis,
                ),
            )
            capturedCount++
            latestSummary = summarize(signedMinor, parsed.currency, parsed.merchant)
            Timber.tag("BankSmsCapture").i("Captured %s %d from %s", parsed.currency, signedMinor, sms.address)
        }

        // Advance the cursor even when nothing parsed, so noise isn't re-scanned forever.
        if (highWaterMark > since) userPreferences.setSmsLastProcessedMillis(highWaterMark)

        if (capturedCount > 0) notifier.notifyCaptured(capturedCount, latestSummary)

        return Result.success()
    }

    private fun summarize(signedMinor: Long, currency: String, merchant: String): String {
        val sign = if (signedMinor < 0) "-" else "+"
        val amount = "%.2f".format(abs(signedMinor) / 100.0)
        return "$sign$currency $amount · $merchant"
    }

    /** Stable per-message id so repeated scans of the same SMS never duplicate an expense. */
    private fun smsTransactionId(sms: InboxSms): String =
        "sms-${sms.dateMillis}-${sms.body.hashCode()}"
}
