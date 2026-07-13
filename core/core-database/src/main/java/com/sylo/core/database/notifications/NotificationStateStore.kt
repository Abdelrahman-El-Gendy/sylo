package com.sylo.core.database.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationStateStore by preferencesDataStore("sylo_notifications")

/** Persists which derived notifications have been read or cleared. */
@Singleton
class NotificationStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val readKey = stringSetPreferencesKey("read_ids")
    private val clearedKey = stringSetPreferencesKey("cleared_ids")

    val readIds: Flow<Set<String>> = context.notificationStateStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[readKey] ?: emptySet() }

    val clearedIds: Flow<Set<String>> = context.notificationStateStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[clearedKey] ?: emptySet() }

    suspend fun markRead(ids: Set<String>) {
        context.notificationStateStore.edit { it[readKey] = (it[readKey] ?: emptySet()) + ids }
    }

    suspend fun clear(ids: Set<String>) {
        context.notificationStateStore.edit { it[clearedKey] = (it[clearedKey] ?: emptySet()) + ids }
    }
}
