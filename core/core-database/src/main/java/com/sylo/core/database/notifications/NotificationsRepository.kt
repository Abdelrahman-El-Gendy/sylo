package com.sylo.core.database.notifications

import com.sylo.core.common.dispatchers.Dispatcher
import com.sylo.core.common.dispatchers.SyloDispatcher
import com.sylo.core.database.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exposes notifications derived from real transactions, with persisted read/cleared
 * state. Everything is reactive: adding a transaction (manual, voice, or an
 * auto-captured bank notification) updates the feed.
 */
@Singleton
class NotificationsRepository @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val stateStore: NotificationStateStore,
    @Dispatcher(SyloDispatcher.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    fun observe(): Flow<List<SyloNotification>> = combine(
        transactionRepository.observeAll(),
        stateStore.readIds,
        stateStore.clearedIds,
    ) { transactions, readIds, clearedIds ->
        NotificationGenerator.generate(transactions, System.currentTimeMillis())
            .filterNot { it.id in clearedIds }
            .map { it.copy(read = it.id in readIds) }
    }.flowOn(defaultDispatcher)

    suspend fun markRead(ids: Set<String>) = stateStore.markRead(ids)

    suspend fun clear(ids: Set<String>) = stateStore.clear(ids)
}
