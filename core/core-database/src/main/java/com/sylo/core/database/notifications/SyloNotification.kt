package com.sylo.core.database.notifications

/** A notification derived from real account activity. No UI types here. */
data class SyloNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val timestampEpochMillis: Long,
    val read: Boolean = false,
)

enum class NotificationKind { INSIGHT, LARGE_EXPENSE, INCOME, SUMMARY, CAPTURED }
