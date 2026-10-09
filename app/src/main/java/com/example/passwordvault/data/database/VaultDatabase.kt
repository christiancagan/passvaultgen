package com.example.passwordvault.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VaultEntryEntity::class],
    version = 1,
    // Export JSON schemas to app/schemas so future migrations can be
    // reviewed against the previous schema (see ksp room.schemaLocation).
    exportSchema = true,
)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun vaultEntryDao(): VaultEntryDao

    companion object {
        @Volatile
        private var instance: VaultDatabase? = null

        fun getInstance(context: Context): VaultDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "passvaultgen.db",
                ).build().also { instance = it }
            }
    }
}
