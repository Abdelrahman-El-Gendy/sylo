package com.sylo.core.database

import com.sylo.core.common.dispatchers.Dispatcher
import com.sylo.core.common.dispatchers.SyloDispatcher
import com.sylo.core.database.dao.TransactionDao
import com.sylo.core.database.entity.TransactionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single read/write path over the encrypted transactions table. Dashboard, History,
 * Analytics, Add-Expense and the voice flow all go through here, so anything written
 * (including a voice-captured expense) shows up everywhere that observes [observeAll].
 */
@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    @Dispatcher(SyloDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    /** Serializes auto-capture check-then-insert across the capture channels. */
    private val captureMutex = Mutex()

    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll().flowOn(ioDispatcher)

    suspend fun getById(id: String): TransactionEntity? = withContext(ioDispatcher) {
        dao.getById(id)
    }

    suspend fun add(transaction: TransactionEntity) = withContext(ioDispatcher) {
        dao.upsert(transaction)
    }

    /**
     * Insert an auto-captured payment only if no equivalent capture (same signed
     * amount + currency, status Auto/SMS) already exists within [dedupeWindowMillis]
     * before its timestamp. This is the single dedup gate for every capture channel:
     * the same real payment surfacing via two SMS apps' notifications, or via the
     * notification listener AND the interval SMS scan, is stored once.
     *
     * The mutex serializes check-then-insert so two channels firing at the same
     * moment can't both pass the existence check.
     *
     * @return true if the transaction was stored, false if it was a duplicate.
     */
    suspend fun addCapturedIfNew(
        transaction: TransactionEntity,
        dedupeWindowMillis: Long,
    ): Boolean = withContext(ioDispatcher) {
        captureMutex.withLock {
            val duplicate = dao.hasCapturedPaymentSince(
                amountMinor = transaction.amountMinor,
                currency = transaction.currency,
                sinceMillis = transaction.timestampEpochMillis - dedupeWindowMillis,
                excludeId = transaction.id,
            )
            if (!duplicate) dao.upsert(transaction)
            !duplicate
        }
    }

    suspend fun delete(id: String) = withContext(ioDispatcher) {
        dao.deleteById(id)
    }
}
