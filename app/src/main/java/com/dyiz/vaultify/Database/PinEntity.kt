package com.dyiz.vaultify.Database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pin_table")
data class PinEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1,
    val pin: String,
    val createdAt: Long = System.currentTimeMillis()
)
