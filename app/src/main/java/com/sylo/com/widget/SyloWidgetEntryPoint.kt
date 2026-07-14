package com.sylo.com.widget

import android.content.Context
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * [SyloBalanceWidget] is instantiated directly by [SyloBalanceWidgetReceiver], not by
 * Hilt, so it can't be constructor-injected. This entry point is the bridge to the
 * same repositories the rest of the app uses, keeping the widget on one source of
 * truth for balance and transactions.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyloWidgetEntryPoint {
    fun transactionRepository(): TransactionRepository
    fun userPreferencesRepository(): UserPreferencesRepository
}

internal fun widgetEntryPoint(context: Context): SyloWidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, SyloWidgetEntryPoint::class.java)
