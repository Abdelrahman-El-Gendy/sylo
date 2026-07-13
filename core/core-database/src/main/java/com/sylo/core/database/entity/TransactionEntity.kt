package com.sylo.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Persistence model for a transaction. Positive [amountMinor] = income, negative = expense. */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amountMinor: Long,
    val currency: String,
    val category: String,
    val note: String?,
    val status: String?,
    val timestampEpochMillis: Long,
    /** Absolute path to an attached receipt image in app-internal storage, if any. */
    val receiptPath: String? = null,
)
