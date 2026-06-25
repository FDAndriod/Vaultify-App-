package com.dyiz.vaultify.data

import com.dyiz.vaultify.Database.NoteDao
import com.dyiz.vaultify.Database.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {

    fun observeAll(): Flow<List<NoteEntity>> = noteDao.observeAll()

    suspend fun count(): Int = withContext(Dispatchers.IO) { noteDao.count() }

    suspend fun getById(id: Long): NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getById(id)
    }

    /** Creates a brand-new note and returns its generated id. */
    suspend fun create(
        title: String,
        content: String,
        colorArgb: Int?,
        pinned: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        noteDao.insert(
            NoteEntity(
                title = title,
                content = content,
                colorArgb = colorArgb,
                pinned = pinned,
                createdAtMs = now,
                updatedAtMs = now
            )
        )
    }

    /**
     * Upserts an existing note. Uses the row's own [NoteEntity.createdAtMs] as the creation
     * stamp so we don't accidentally overwrite it on each edit, and bumps [updatedAtMs] so the
     * list re-sorts to show recently edited notes first.
     */
    suspend fun update(
        id: Long,
        title: String,
        content: String,
        colorArgb: Int?,
        pinned: Boolean
    ) = withContext(Dispatchers.IO) {
        val existing = noteDao.getById(id) ?: return@withContext
        noteDao.update(
            existing.copy(
                title = title,
                content = content,
                colorArgb = colorArgb,
                pinned = pinned,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun togglePin(id: Long) = withContext(Dispatchers.IO) {
        val existing = noteDao.getById(id) ?: return@withContext
        noteDao.update(existing.copy(pinned = !existing.pinned, updatedAtMs = System.currentTimeMillis()))
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteById(id)
    }
    suspend fun deleteAllNotes() = noteDao.deleteAll()

}
