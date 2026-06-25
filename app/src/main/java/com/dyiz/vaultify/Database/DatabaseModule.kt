package com.dyiz.vaultify.Database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vaultify_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun providePinDao(database: AppDatabase): PinDao {
        return database.pinDao()
    }

    @Provides
    @Singleton
    fun provideSecurityQuestionDao(database: AppDatabase): SecurityQuestionDao {
        return database.securityQuestionDao()
    }

    @Provides
    @Singleton
    fun provideVaultItemDao(database: AppDatabase): VaultItemDao {
        return database.vaultItemDao()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }
}
