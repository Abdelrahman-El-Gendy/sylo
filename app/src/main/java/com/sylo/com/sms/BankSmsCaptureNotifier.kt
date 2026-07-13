package com.sylo.com.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sylo.com.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a status-bar notification when the SMS scan auto-captures new expenses, so
 * the capture is visible in real time (mirroring the app's in-app notifications feed).
 */
@Singleton
class BankSmsCaptureNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    /**
     * @param count number of expenses captured in this scan
     * @param summary a short line describing the most recent capture (e.g. "-EGP 250.00 · Carrefour")
     */
    fun notifyCaptured(count: Int, summary: String) {
        // Respects the user's system notification setting (and the POST_NOTIFICATIONS
        // runtime grant on API 33+); silently no-ops if notifications are disabled.
        if (!manager.areNotificationsEnabled()) return

        ensureChannel()

        // Immutable PendingIntent that just opens the app (no nested/untrusted intent).
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val title = if (count == 1) "Expense captured from SMS" else "$count expenses captured from SMS"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sms)
            .setColor(BRAND_CYAN)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bank SMS capture",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts when Sylo logs an expense read from a bank SMS."
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "sms_capture"
        const val NOTIFICATION_ID = 4201
        const val BRAND_CYAN = 0xFF00DBE9.toInt()
    }
}
