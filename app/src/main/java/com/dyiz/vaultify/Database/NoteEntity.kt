package com.dyiz.vaultify.Database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A private note / diary entry living entirely inside the vault's Room database. The DB file is
 * stored in app-private storage, so on a modern Android build with file-based encryption the body
 * is already protected by the device lock; Vaultify adds the PIN gate on top of that.
 */
@Entity(tableName = "note_table")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    /** Packed ARGB color used for the note card background (nullable = default app color). */
    val colorArgb: Int? = null,
    val pinned: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
