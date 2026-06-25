package com.dyiz.vaultify.Database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VaultItemDao {

    @Insert
    suspend fun insert(entity: VaultItemEntity): Long

    @Query("SELECT * FROM vault_item_table WHERE type = :type ORDER BY dateAddedMs DESC")
    suspend fun getAllByType(type: String): List<VaultItemEntity>

    @Query("SELECT * FROM vault_item_table WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VaultItemEntity?

    @Delete
    suspend fun delete(entity: VaultItemEntity)

    @Query("DELETE FROM vault_item_table WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM vault_item_table WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM vault_item_table WHERE type = :type")
    suspend fun countByType(type: String): Int
}
