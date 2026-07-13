package com.sylo.com.sms

import android.content.Context
import android.provider.Telephony
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/** A single SMS read from the device inbox. */
data class InboxSms(
    val address: String,
    val body: String,
    val dateMillis: Long,
)

/**
 * Reads the device SMS inbox (`content://sms/inbox`) for bank-statement messages.
 *
 * This is read-only polling driven by [BankSmsScanWorker] — deliberately NOT a
 * real-time `SMS_RECEIVED` broadcast receiver (that role is covered by the
 * notification listener). Requires the `READ_SMS` runtime permission; the query is
 * fully parameterized so the date/selection can never be string-injected.
 */
@Singleton
class BankSmsScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Returns inbox messages received **today** and strictly **after** [sinceMillis]
     * (the last-processed high-water mark), oldest first. Same-day scoping keeps the
     * import focused on statements as they arrive through the day.
     *
     * Returns an empty list if `READ_SMS` isn't granted or the provider is unavailable.
     */
    fun readTodaysMessagesSince(sinceMillis: Long, nowMillis: Long): List<InboxSms> {
        val floor = maxOf(sinceMillis, startOfDay(nowMillis))
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        // Parameterized selection — value supplied via selectionArgs, never concatenated.
        val selection = "${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf(floor.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        return runCatching {
            val out = mutableListOf<InboxSms>()
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder,
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    out += InboxSms(
                        address = cursor.getString(addressIdx).orEmpty(),
                        body = cursor.getString(bodyIdx).orEmpty(),
                        dateMillis = cursor.getLong(dateIdx),
                    )
                }
            }
            out
        }.getOrElse { e ->
            Timber.tag("BankSmsScanner").w(e, "SMS inbox read failed (permission or provider)")
            emptyList()
        }
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
