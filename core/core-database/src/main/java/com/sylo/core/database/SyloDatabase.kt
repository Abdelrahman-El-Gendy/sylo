package com.sylo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sylo.core.database.dao.TransactionDao
import com.sylo.core.database.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class],
    version = 3,
    // Set to true and configure room.schemaLocation once migrations are introduced.
    exportSchema = false,
)
abstract class SyloDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val NAME = "sylo-encrypted.db"
    }
}
