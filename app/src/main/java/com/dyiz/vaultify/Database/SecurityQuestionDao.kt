package com.dyiz.vaultify.Database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SecurityQuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SecurityQuestionEntity)

    @Query("SELECT * FROM security_question_table WHERE id = 1 LIMIT 1")
    suspend fun get(): SecurityQuestionEntity?
}
