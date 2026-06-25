package com.dyiz.vaultify.Database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_item_table")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val localFilePath: String,
    val displayName: String,
    val mimeType: String?,
    val dateAddedMs: Long = System.currentTimeMillis(),
    /** Original folder path (e.g. Pictures/Wallpapers) so we restore to same location on unhide. */
    val originalRelativePath: String? = null
)
