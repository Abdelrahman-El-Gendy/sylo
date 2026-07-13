package com.sylo.core.database.di

import android.content.Context
import androidx.room.Room
import com.sylo.core.database.SyloDatabase
import com.sylo.core.database.dao.TransactionDao
import com.sylo.core.database.security.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

/**
 * Builds the Room database on top of a SQLCipher-encrypted file. The passphrase is
 * supplied by [DatabaseKeyProvider] (implemented in :core-security), so the raw key
 * never lives in this module.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): SyloDatabase {
        // The sqlcipher-android artifact does NOT auto-load its native lib; load it
        // once before opening any connection or nativeOpn() throws UnsatisfiedLinkError.
        System.loadLibrary("sqlcipher")

        val factory = SupportOpenHelperFactory(keyProvider.getDatabasePassphrase())

        return Room.databaseBuilder(context, SyloDatabase::class.java, SyloDatabase.NAME)
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideTransactionDao(database: SyloDatabase): TransactionDao =
        database.transactionDao()
}
