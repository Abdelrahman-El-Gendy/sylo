package com.sylo.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sylo.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestampEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    /**
     * Whether an auto-captured row (notification listener or SMS scan) with the same
     * signed amount and currency already exists at/after [sinceMillis]. Both capture
     * paths consult this before inserting, so the same real-world payment seen through
     * different channels (two SMS apps' notifications, listener + interval scan)
     * lands only once. [excludeId] lets the SMS worker re-upsert its own row.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM transactions
            WHERE status IN ('Auto', 'SMS')
              AND amountMinor = :amountMinor
              AND currency = :currency
              AND timestampEpochMillis >= :sinceMillis
              AND id != :excludeId
        )
        """,
    )
    suspend fun hasCapturedPaymentSince(
        amountMinor: Long,
        currency: String,
        sinceMillis: Long,
        excludeId: String,
    ): Boolean

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)
}
