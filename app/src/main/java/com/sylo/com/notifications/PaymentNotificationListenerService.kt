package com.sylo.com.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sylo.core.common.payments.PaymentNotificationParser
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.entity.TransactionEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Listens to notifications posted by OTHER apps (banks, InstaPay, wallets…), parses
 * any that look like a payment via [PaymentNotificationParser], and auto-records them
 * into the encrypted local DB. Requires the user to grant "Notification access" in
 * system settings (see the Settings screen).
 */
@AndroidEntryPoint
class PaymentNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var repository: TransactionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // De-dupe: banks often (re)post the same notification. Remember recent keys briefly.
    private val recent = LinkedHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return                 // ignore our own notifications

        if (sbn.isOngoing) return                       // ignore persistent/service notifications

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()

        val parsed = PaymentNotificationParser.parse(pkg, title, text) ?: return

        val key = "$pkg|${parsed.amountMinor}|${parsed.rawText}"
        if (isDuplicate(key)) return

        Timber.tag("PaymentCapture").i("Captured %s %d from %s", parsed.currency, parsed.amountMinor, pkg)
        scope.launch {
            repository.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    title = parsed.merchant,
                    amountMinor = if (parsed.isIncome) parsed.amountMinor else -parsed.amountMinor,
                    currency = parsed.currency,
                    category = parsed.category,
                    note = "${parsed.sourceApp}: ${parsed.rawText}",
                    status = "Auto",
                    timestampEpochMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun isDuplicate(key: String): Boolean {
        val now = System.currentTimeMillis()
        recent.entries.removeAll { now - it.value > DEDUPE_WINDOW_MS }
        if (recent.containsKey(key)) return true
        recent[key] = now
        if (recent.size > MAX_RECENT) recent.remove(recent.keys.first())
        return false
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val DEDUPE_WINDOW_MS = 60_000L
        const val MAX_RECENT = 50
    }
}
