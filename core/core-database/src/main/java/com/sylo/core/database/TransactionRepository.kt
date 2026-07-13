package com.sylo.core.database

import com.sylo.core.common.dispatchers.Dispatcher
import com.sylo.core.common.dispatchers.SyloDispatcher
import com.sylo.core.database.dao.TransactionDao
import com.sylo.core.database.entity.TransactionEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
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
    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll().flowOn(ioDispatcher)

    suspend fun getById(id: String): TransactionEntity? = withContext(ioDispatcher) {
        dao.getById(id)
    }

    suspend fun add(transaction: TransactionEntity) = withContext(ioDispatcher) {
        dao.upsert(transaction)
    }

    suspend fun delete(id: String) = withContext(ioDispatcher) {
        dao.deleteById(id)
    }
}
