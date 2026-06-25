package com.dyiz.vaultify.Database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NoteEntity): Long

    @Update
    suspend fun update(entity: NoteEntity)

    @Delete
    suspend fun delete(entity: NoteEntity)

    @Query("DELETE FROM note_table")
    suspend fun deleteAll()

    @Query("DELETE FROM note_table WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM note_table WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteEntity?

    /**
     * Pinned notes first, then newest-updated first. Emitted as a Flow so the list screen updates
     * live when the user edits, pins, or deletes a note.
     */
    @Query("SELECT * FROM note_table ORDER BY pinned DESC, updatedAtMs DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM note_table")
    suspend fun count(): Int
}
