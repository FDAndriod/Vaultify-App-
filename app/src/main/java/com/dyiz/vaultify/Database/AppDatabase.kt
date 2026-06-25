package com.dyiz.vaultify.Database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PinEntity::class, SecurityQuestionEntity::class, VaultItemEntity::class, NoteEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pinDao(): PinDao
    abstract fun securityQuestionDao(): SecurityQuestionDao
    abstract fun vaultItemDao(): VaultItemDao
    abstract fun noteDao(): NoteDao
}
