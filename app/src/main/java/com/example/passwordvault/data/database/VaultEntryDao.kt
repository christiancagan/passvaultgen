package com.example.passwordvault.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultEntryDao {

    @Query("SELECT * FROM vault_entries ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultEntryEntity?

    @Query("SELECT * FROM vault_entries")
    suspend fun getAll(): List<VaultEntryEntity>

    @Query(
        "SELECT * FROM vault_entries WHERE title LIKE '%' || :query || '%' " +
            "OR category LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC",
    )
    fun search(query: String): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE category = :category ORDER BY title COLLATE NOCASE ASC")
    fun observeByCategory(category: String): Flow<List<VaultEntryEntity>>

    @Query("SELECT DISTINCT category FROM vault_entries ORDER BY category COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VaultEntryEntity>)

    /** Atomically replaces all entries. Used by backup restore (transaction rollback on failure). */
    @Transaction
    suspend fun replaceAll(entries: List<VaultEntryEntity>) {
        deleteAll()
        insertAll(entries)
    }

    @Update
    suspend fun update(entry: VaultEntryEntity)

    @Delete
    suspend fun delete(entry: VaultEntryEntity)

    @Query("DELETE FROM vault_entries")
    suspend fun deleteAll()
}
